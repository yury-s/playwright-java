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

import java.util.*;

/**
 * Keyboard provides an api for managing a virtual keyboard. The high level api is keyboard.type(text[, options]), which takes raw
 * <p>
 * characters and generates proper keydown, keypress/input, and keyup events on your page.
 * <p>
 * For finer control, you can use keyboard.down(key), keyboard.up(key), and keyboard.insertText(text) to manually fire
 * <p>
 * events as if they were generated from a real keyboard.
 * <p>
 * An example to trigger select-all with the keyboard
 * <p>
 */
public interface Keyboard {
  enum Modifier { ALT, CONTROL, META, SHIFT }

  /**
   * Dispatches a {@code keydown} event.
   * <p>
   * {@code key} can specify the intended keyboardEvent.key
   * <p>
   * value or a single character to generate the text for. A superset of the {@code key} values can be found
   * <p>
   * here. Examples of the keys are:
   * <p>
   * {@code F1} - {@code F12}, {@code Digit0}- {@code Digit9}, {@code KeyA}- {@code KeyZ}, {@code Backquote}, {@code Minus}, {@code Equal}, {@code Backslash}, {@code Backspace}, {@code Tab},
   * <p>
   * {@code Delete}, {@code Escape}, {@code ArrowDown}, {@code End}, {@code Enter}, {@code Home}, {@code Insert}, {@code PageDown}, {@code PageUp}, {@code ArrowRight}, {@code ArrowUp}, etc.
   * <p>
   * Following modification shortcuts are also suported: {@code Shift}, {@code Control}, {@code Alt}, {@code Meta}, {@code ShiftLeft}.
   * <p>
   * Holding down {@code Shift} will type the text that corresponds to the {@code key} in the upper case.
   * <p>
   * If {@code key} is a single character, it is case-sensitive, so the values {@code a} and {@code A} will generate different respective
   * <p>
   * texts.
   * <p>
   * If {@code key} is a modifier key, {@code Shift}, {@code Meta}, {@code Control}, or {@code Alt}, subsequent key presses will be sent with that modifier
   * <p>
   * active. To release the modifier key, use keyboard.up(key).
   * <p>
   * After the key is pressed once, subsequent calls to keyboard.down(key) will have
   * <p>
   * repeat set to true. To release the key, use
   * <p>
   * keyboard.up(key).
   * <p>
   * <strong>NOTE</strong> Modifier keys DO influence {@code keyboard.down}. Holding down {@code Shift} will type the text in upper case.
   * @param key Name of the key to press or a character to generate, such as {@code ArrowLeft} or {@code a}.
   */
  void down(String key);
  /**
   * Dispatches only {@code input} event, does not emit the {@code keydown}, {@code keyup} or {@code keypress} events.
   * <p>
   * 
   * <p>
   * <strong>NOTE</strong> Modifier keys DO NOT effect {@code keyboard.insertText}. Holding down {@code Shift} will not type the text in upper case.
   * @param text Sets input to the specified text value.
   */
  void insertText(String text);
  default void press(String key) {
    press(key, 0);
  }
  /**
   * {@code key} can specify the intended keyboardEvent.key
   * <p>
   * value or a single character to generate the text for. A superset of the {@code key} values can be found
   * <p>
   * here. Examples of the keys are:
   * <p>
   * {@code F1} - {@code F12}, {@code Digit0}- {@code Digit9}, {@code KeyA}- {@code KeyZ}, {@code Backquote}, {@code Minus}, {@code Equal}, {@code Backslash}, {@code Backspace}, {@code Tab},
   * <p>
   * {@code Delete}, {@code Escape}, {@code ArrowDown}, {@code End}, {@code Enter}, {@code Home}, {@code Insert}, {@code PageDown}, {@code PageUp}, {@code ArrowRight}, {@code ArrowUp}, etc.
   * <p>
   * Following modification shortcuts are also suported: {@code Shift}, {@code Control}, {@code Alt}, {@code Meta}, {@code ShiftLeft}.
   * <p>
   * Holding down {@code Shift} will type the text that corresponds to the {@code key} in the upper case.
   * <p>
   * If {@code key} is a single character, it is case-sensitive, so the values {@code a} and {@code A} will generate different respective
   * <p>
   * texts.
   * <p>
   * Shortcuts such as {@code key: "Control+o"} or {@code key: "Control+Shift+T"} are supported as well. When speficied with the
   * <p>
   * modifier, modifier is pressed and being held while the subsequent key is being pressed.
   * <p>
   * Shortcut for keyboard.down(key) and keyboard.up(key).
   * @param key Name of the key to press or a character to generate, such as {@code ArrowLeft} or {@code a}.
   */
  void press(String key, int delay);
  default void type(String text) {
    type(text, 0);
  }
  /**
   * Sends a {@code keydown}, {@code keypress}/{@code input}, and {@code keyup} event for each character in the text.
   * <p>
   * To press a special key, like {@code Control} or {@code ArrowDown}, use keyboard.press(key[, options]).
   * <p>
   * 
   * <p>
   * <strong>NOTE</strong> Modifier keys DO NOT effect {@code keyboard.type}. Holding down {@code Shift} will not type the text in upper case.
   * @param text A text to type into a focused element.
   */
  void type(String text, int delay);
  /**
   * Dispatches a {@code keyup} event.
   * @param key Name of the key to press or a character to generate, such as {@code ArrowLeft} or {@code a}.
   */
  void up(String key);
}

