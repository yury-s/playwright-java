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

import java.nio.file.Path;
import java.util.*;

/**
 * Chromium-specific features including Tracing, service worker support, etc.
 * <p>
 * You can use {@code chromiumBrowser.startTracing} and {@code chromiumBrowser.stopTracing} to create a trace file which can be opened in Chrome DevTools or timeline viewer.
 * <p>
 */
public interface ChromiumBrowser extends Browser {
  class StartTracingOptions {
    /**
     * A path to write the trace file to.
     */
    public Path path;
    /**
     * captures screenshots in the trace.
     */
    public Boolean screenshots;
    /**
     * specify custom categories to use instead of default.
     */
    public List<String> categories;

    public StartTracingOptions withPath(Path path) {
      this.path = path;
      return this;
    }
    public StartTracingOptions withScreenshots(Boolean screenshots) {
      this.screenshots = screenshots;
      return this;
    }
    public StartTracingOptions withCategories(List<String> categories) {
      this.categories = categories;
      return this;
    }
  }
  /**
   * 
   * @return Promise that resolves to the newly created browser
   * session.
   */
  CDPSession newBrowserCDPSession();
  default void startTracing(Page page) {
    startTracing(page, null);
  }
  default void startTracing() {
    startTracing(null);
  }
  /**
   * Only one trace can be active at a time per browser.
   * @param page Optional, if specified, tracing includes screenshots of the given page.
   */
  void startTracing(Page page, StartTracingOptions options);
  /**
   * 
   * @return Promise which resolves to buffer with trace data.
   */
  byte[] stopTracing();
}

