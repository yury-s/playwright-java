/*
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.playwright;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import static com.microsoft.playwright.Page.EventType.FRAMENAVIGATED;
import static com.microsoft.playwright.Utils.expectedSSLError;
import static com.microsoft.playwright.Utils.getOS;
import static org.junit.jupiter.api.Assertions.*;

public class TestPageWaitForNavigation extends TestBase {

  @Test
  void shouldWork() {
    page.navigate(server.EMPTY_PAGE);
    Deferred<Response> response = page.futureNavigation();
    page.evaluate("url => window.location.href = url", server.PREFIX + "/grid.html");
    assertTrue(response.get().ok());
    assertTrue(response.get().url().contains("grid.html"));
  }

  @Test
  void shouldRespectTimeout() {
    Deferred<Response> promise = page.futureNavigation(new Page.FutureNavigationOptions().withUrl("**/frame.html").withTimeout(5000));
    page.navigate(server.EMPTY_PAGE);
    try {
      promise.get();
      fail("did not throw");
    } catch (PlaywrightException e) {
//      assertTrue(e.getMessage().contains("page.futureNavigation: Timeout 5000ms exceeded."));
      assertTrue(e.getMessage().contains("Timeout 5000ms exceeded"));
//      assertTrue(e.getMessage().contains("waiting for navigation to '**/frame.html' until 'load'"));
//      assertTrue(e.getMessage().contains("navigated to '${server.EMPTY_PAGE}'"));
    }
  }

  // Skipped in sync API.
  void shouldWorkWithBothDomcontentloadedAndLoad() {
  }

  @Test
  void shouldWorkWithClickingOnAnchorLinks() {
    page.navigate(server.EMPTY_PAGE);
    page.setContent("<a href='#foobar'>foobar</a>");
    Deferred<Response> response = page.futureNavigation();
    page.click("a");
    assertNull(response.get());
    assertEquals(server.EMPTY_PAGE + "#foobar", page.url());
  }

  @Test
  void shouldWorkWithClickingOnLinksWhichDoNotCommitNavigation() throws InterruptedException {
    page.navigate(server.EMPTY_PAGE);
    Deferred<Response> event = page.futureNavigation();
    page.setContent("<a href='" + httpsServer.EMPTY_PAGE + "'>foobar</a>");
    try {
      page.click("a");
      event.get();
      fail("did not throw");
    } catch (PlaywrightException e) {
      // TODO: figure out why it is inconsistent on Linux WebKit.
      assertTrue(e.getMessage().contains(expectedSSLError(browserType.name())) ||
          (isWebKit() && getOS() == Utils.OS.LINUX && "Server required TLS certificate".equals(e.getMessage())),
        "Unexpected exception: '" + e.getMessage() + "'");
    }
  }

  @Test
  void shouldWorkWithHistoryPushState() {
    page.navigate(server.EMPTY_PAGE);
    page.setContent("<a onclick='javascript:pushState()'>SPA</a>\n" +
      "<script>\n" +
      "  function pushState() { history.pushState({}, '', 'wow.html') }\n" +
      "</script>");
    Deferred<Response> response = page.futureNavigation();
    page.click("a");
    assertNull(response.get());
    assertEquals(server.PREFIX + "/wow.html", page.url());
  }

  @Test
  void shouldWorkWithHistoryReplaceState() {
    page.navigate(server.EMPTY_PAGE);
    page.setContent(" <a onclick='javascript:replaceState()'>SPA</a>\n" +
      "<script>\n" +
      "  function replaceState() { history.replaceState({}, '', '/replaced.html') }\n" +
      "</script>");
    Deferred<Response> response = page.futureNavigation();
    page.click("a");
    assertNull(response.get());
    assertEquals(server.PREFIX + "/replaced.html", page.url());
  }

  @Test
  void shouldWorkWithDOMHistoryBackHistoryForward() {
    page.navigate(server.EMPTY_PAGE);
    page.setContent("<a id=back onclick='javascript:goBack()'>back</a>\n" +
      "<a id=forward onclick='javascript:goForward()'>forward</a>\n" +
      "<script>\n" +
      "  function goBack() { history.back(); }\n" +
      "  function goForward() { history.forward(); }\n" +
      "  history.pushState({}, '', '/first.html');\n" +
      "  history.pushState({}, '', '/second.html');\n" +
      "</script>");
    assertEquals(server.PREFIX + "/second.html", page.url());

    Deferred<Response> backResponse = page.futureNavigation();
    page.click("a#back");
    assertNull(backResponse.get());
    assertEquals(server.PREFIX + "/first.html", page.url());

    Deferred<Response> forwardResponse = page.futureNavigation();
    page.click("a#forward");
    assertNull(forwardResponse.get());
    assertEquals(server.PREFIX + "/second.html", page.url());
  }

  @Test
  void shouldWorkWhenSubframeIssuesWindowStop() {
    server.setRoute("/frames/style.css", exchange -> {});
    boolean[] frameWindowStopCalled = {false};
    page.addListener(Page.EventType.FRAMEATTACHED, event -> {
      Frame frame = (Frame) event.data();
      page.addListener(FRAMENAVIGATED, event1 -> {
        if (frame.equals(event1.data())) {
          frame.evaluate("window.stop()");
          frameWindowStopCalled[0] = true;
        }
      });
    });
    page.navigate(server.PREFIX + "/frames/one-frame.html");
    assertTrue(frameWindowStopCalled[0]);
  }

  @Test
  void shouldWorkWithUrlMatch() {
    page.navigate(server.EMPTY_PAGE);

    Deferred<Response> response1 = page.futureNavigation(new Page.FutureNavigationOptions().withUrl("**/one-style.html"));
    page.navigate(server.PREFIX + "/one-style.html");
    assertNotNull(response1.get());
    assertEquals(server.PREFIX + "/one-style.html", response1.get().url());

    Deferred<Response> response2 = page.futureNavigation(new Page.FutureNavigationOptions().withUrl(Pattern.compile("frame.html$")));
    page.navigate(server.PREFIX + "/frame.html");
    assertNotNull(response2.get());
    assertEquals(server.PREFIX + "/frame.html", response2.get().url());

    Deferred<Response> response3 = page.futureNavigation(new Page.FutureNavigationOptions().withUrl(url -> {
      try {
        return new URL(url).getQuery().contains("foo=bar");
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }));
    page.navigate(server.PREFIX + "/frame.html?foo=bar");
    assertNotNull(response3.get());
    assertEquals(server.PREFIX + "/frame.html?foo=bar", response3.get().url());
  }

  @Test
  void shouldWorkWithUrlMatchForSameDocumentNavigations() {
    page.navigate(server.EMPTY_PAGE);
    Deferred<Response> waitPromise = page.futureNavigation(new Page.FutureNavigationOptions().withUrl("**/third.html"));
    page.evaluate("() => {\n" +
      "  history.pushState({}, '', '/first.html');\n" +
      "}");
    page.evaluate("() => {\n" +
      "  history.pushState({}, '', '/second.html');\n" +
      "}");
    page.evaluate("() => {\n" +
      "  history.pushState({}, '', '/third.html');\n" +
      "}");
    assertNull(waitPromise.get());
  }

  @Test
  void shouldWorkForCrossProcessNavigations() {
    page.navigate(server.EMPTY_PAGE);
    Deferred<Response> waitPromise = page.futureNavigation(new Page.FutureNavigationOptions().withWaitUntil(Frame.LoadState.DOMCONTENTLOADED));
    String url = server.CROSS_PROCESS_PREFIX + "/empty.html";
    page.navigate(url);
    Response response = waitPromise.get();
    assertEquals(url, response.url());
    assertEquals(url, page.url());
    assertEquals(url, page.evaluate("document.location.href"));
  }

  @Test
  void shouldWorkOnFrame() {
    page.navigate(server.PREFIX + "/frames/one-frame.html");
    Frame frame = page.frames().get(1);
    Deferred<Response> response = frame.futureNavigation();
    frame.evaluate("url => window.location.href = url", server.PREFIX + "/grid.html");
    assertTrue(response.get().ok());
    assertTrue(response.get().url().contains("grid.html"));
    assertEquals(frame, response.get().frame());
    assertTrue(page.url().contains("/frames/one-frame.html"));
  }

  @Test
  void shouldFailWhenFrameDetaches() throws InterruptedException {
    page.navigate(server.PREFIX + "/frames/one-frame.html");
    Frame frame = page.frames().get(1);
    server.setRoute("/empty.html", exchange -> {});
    try {
      Deferred<Response> response = frame.futureNavigation();
      page.evaluate("() => {\n" +
        "  frames[0].location.href = '/empty.html';\n" +
        "  setTimeout(() => document.querySelector('iframe').remove());\n" +
        "}\n");
      response.get();
      fail("did not throw");
    } catch (PlaywrightException e) {
//      assertTrue(e.getMessage().contains("waiting for navigation until \"load\""));
      assertTrue(e.getMessage().contains("frame was detached"));
    }
  }


}
