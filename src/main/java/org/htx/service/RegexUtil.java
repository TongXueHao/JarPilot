/**
 * MIT License
 *
 * Copyright (c) 2025 Hao Tong Xue
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.htx.service;

/** Utility class for regex-based validations.
 *
 * @Author Hao Tong Xue
 * @Date 2025/8/27 15:00
 * @Version 1.0
 */
public final class RegexUtil {

    /**
     * check if the given path is a valid Linux file path.
     * @param path the file path to check
     * @return  true if valid, false otherwise
     */
    public static boolean isValidLinuxPath(String path) {
        if (path == null) return false;
        String regex = "^\\/(?=.*[A-Za-z])[A-Za-z0-9._\\-/]+$";
        return path.matches(regex);
    }

    /**
     * check if the given profile is valid.
     * @param profile the profile to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidProfile(String profile) {
        return profile != null && profile.matches("^[a-zA-Z0-9_-]+(,[a-zA-Z0-9_-]+)*$");
    }



}
