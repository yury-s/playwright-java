#!/usr/bin/env node
// Probe `chrome-headless-shell` for the jammy x86_64 startup SIGSEGV.
// Launches the binary in a tight loop with the same flags Playwright uses,
// keeps fd 3 / fd 4 open as pipes (matches `--remote-debugging-pipe`),
// and counts how many launches die with SIGSEGV during startup.

'use strict';

const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const CHROME = process.env.CHROME ||
  '/opt/chrome-headless-shell-linux64/chrome-headless-shell';
const ITERATIONS = Number(process.env.ITERATIONS || 200);
const STARTUP_WAIT_MS = Number(process.env.STARTUP_WAIT_MS || 2500);
const PARALLEL = Math.max(1, Number(process.env.PARALLEL || 1));
const VERBOSE = !!process.env.VERBOSE;

// The exact flag set Playwright passes when launching chromium with
// `--remote-debugging-pipe` on Linux. Kept verbatim so the reproducer
// hits the same code paths as the failing CI runs.
const FLAGS = [
  '--disable-field-trial-config',
  '--disable-background-networking',
  '--disable-background-timer-throttling',
  '--disable-backgrounding-occluded-windows',
  '--disable-back-forward-cache',
  '--disable-breakpad',
  '--disable-client-side-phishing-detection',
  '--disable-component-extensions-with-background-pages',
  '--disable-component-update',
  '--no-default-browser-check',
  '--disable-default-apps',
  '--disable-dev-shm-usage',
  '--disable-edgeupdater',
  '--disable-extensions',
  '--disable-features=AvoidUnnecessaryBeforeUnloadCheckSync,BoundaryEventDispatchTracksNodeRemoval,DestroyProfileOnBrowserClose,DialMediaRouteProvider,GlobalMediaControls,HttpsUpgrades,LensOverlay,MediaRouter,PaintHolding,ThirdPartyStoragePartitioning,Translate,AutoDeElevate,RenderDocument,OptimizationHints,msForceBrowserSignIn,msEdgeUpdateLaunchServicesPreferredVersion',
  '--enable-features=CDPScreenshotNewSurface',
  '--allow-pre-commit-input',
  '--disable-hang-monitor',
  '--disable-ipc-flooding-protection',
  '--disable-popup-blocking',
  '--disable-prompt-on-repost',
  '--disable-renderer-backgrounding',
  '--force-color-profile=srgb',
  '--metrics-recording-only',
  '--no-first-run',
  '--password-store=basic',
  '--use-mock-keychain',
  '--no-service-autorun',
  '--export-tagged-pdf',
  '--disable-search-engine-choice-screen',
  '--unsafely-disable-devtools-self-xss-warnings',
  '--edge-skip-compat-layer-relaunch',
  '--disable-infobars',
  '--disable-search-engine-choice-screen',
  '--disable-sync',
  '--enable-unsafe-swiftshader',
  '--headless',
  '--hide-scrollbars',
  '--mute-audio',
  '--blink-settings=primaryHoverType=2,availableHoverTypes=2,primaryPointerType=4,availablePointerTypes=4',
  '--no-sandbox',
  '--remote-debugging-pipe',
  '--no-startup-window',
];

function safe(cmd) {
  try {
    return execSync(cmd, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
  } catch (_) {
    return '?';
  }
}

function isSegv(signal, stderr) {
  if (signal === 'SIGSEGV') return true;
  if (!stderr) return false;
  return /Received signal 11|signal=SIGSEGV/.test(stderr);
}

function runOnce() {
  return new Promise((resolve) => {
    const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'chs-'));
    const args = FLAGS.concat([`--user-data-dir=${userDataDir}`]);

    let child;
    try {
      child = spawn(CHROME, args, {
        // stdin ignored; stdout/stderr captured; fds 3 + 4 are pipes
        // so chromium's --remote-debugging-pipe code path sees real pipes.
        stdio: ['ignore', 'pipe', 'pipe', 'pipe', 'pipe'],
      });
    } catch (err) {
      resolve({ spawnError: err, stderr: '' });
      return;
    }

    let stderr = '';
    child.stderr.on('data', (d) => { stderr += d.toString(); });
    child.stdout.on('data', () => { /* drain */ });
    // chromium writes CDP framing to fd 4; drain so the buffer doesn't fill.
    if (child.stdio[4] && child.stdio[4].on)
      child.stdio[4].on('data', () => {});

    const killTimer = setTimeout(() => {
      try { child.kill('SIGTERM'); } catch (_) {}
      setTimeout(() => { try { child.kill('SIGKILL'); } catch (_) {} }, 500);
    }, STARTUP_WAIT_MS);

    child.on('error', (err) => {
      clearTimeout(killTimer);
      try { fs.rmSync(userDataDir, { recursive: true, force: true }); } catch (_) {}
      resolve({ spawnError: err, stderr });
    });

    child.on('exit', (code, signal) => {
      clearTimeout(killTimer);
      try { fs.rmSync(userDataDir, { recursive: true, force: true }); } catch (_) {}
      resolve({ code, signal, stderr });
    });
  });
}

async function main() {
  console.log(`chrome-headless-shell startup-crash repro`);
  console.log(`  uname -a:   ${safe('uname -a')}`);
  console.log(`  os-release: ${safe("grep PRETTY_NAME /etc/os-release | cut -d= -f2-")}`);
  console.log(`  arch:       ${os.arch()}`);
  console.log(`  libc:       ${safe('ldd --version | head -1')}`);
  console.log(`  binary:     ${CHROME}`);
  console.log(`  version:    ${safe(`"${CHROME}" --version`)}`);
  console.log(`  iterations: ${ITERATIONS}`);
  console.log(`  parallel:   ${PARALLEL}`);
  console.log(`  wait/iter:  ${STARTUP_WAIT_MS}ms`);
  console.log('');

  if (!fs.existsSync(CHROME)) {
    console.error(`ERROR: ${CHROME} not found. Set CHROME=/path/to/chrome-headless-shell`);
    process.exit(2);
  }

  let crashes = 0;
  let other = 0;
  let firstCrash = null;
  let printed = 0;
  const t0 = Date.now();

  function consume(i, r) {
    if (r.spawnError) {
      console.error(`\nspawn error on iter ${i}: ${r.spawnError.message}`);
      process.exit(2);
    }
    if (isSegv(r.signal, r.stderr)) {
      crashes++;
      if (!firstCrash) firstCrash = Object.assign({ i: i }, r);
      process.stdout.write('X');
      if (VERBOSE) {
        process.stdout.write(`\n--- iter ${i} stderr ---\n${r.stderr}\n--- end ---\n`);
      }
    } else if (r.signal && r.signal !== 'SIGTERM' && r.signal !== 'SIGKILL') {
      other++;
      process.stdout.write('?');
    } else {
      process.stdout.write('.');
    }
    printed++;
    if (printed % 50 === 0) process.stdout.write(` ${printed}\n`);
  }

  // Run in batches of size PARALLEL — concurrent launches stress the kernel
  // process-creation path the same way Maven Surefire does in CI.
  let i = 1;
  while (i <= ITERATIONS) {
    const batch = Math.min(PARALLEL, ITERATIONS - i + 1);
    const promises = [];
    for (let k = 0; k < batch; k++) {
      const idx = i + k;
      promises.push(runOnce().then((r) => ({ idx: idx, r: r })));
    }
    const results = await Promise.all(promises);
    for (const { idx, r } of results) consume(idx, r);
    i += batch;
  }

  const elapsed = ((Date.now() - t0) / 1000).toFixed(1);
  console.log(`\n\nResults after ${ITERATIONS} launches in ${elapsed}s:`);
  console.log(`  SIGSEGV crashes:        ${crashes} (${(crashes / ITERATIONS * 100).toFixed(2)}%)`);
  console.log(`  Other unexpected exits: ${other}`);
  if (firstCrash) {
    console.log(`\nFirst crash on iteration ${firstCrash.i} (signal=${firstCrash.signal}, code=${firstCrash.code}):`);
    console.log('---- stderr ----');
    console.log(firstCrash.stderr);
    console.log('---- end ----');
  }
  process.exit(crashes ? 1 : 0);
}

main().catch((err) => { console.error(err); process.exit(2); });
