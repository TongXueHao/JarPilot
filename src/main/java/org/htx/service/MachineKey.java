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

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.security.MessageDigest;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * Utility class to generate a machine-specific AES key based on MAC address or fallback identifiers.
 *
 * @Author Hao Tong Xue
 * @Date 2025/9/17 10:30
 * @Version 1.0
 */
public class MachineKey {

    public static String getAesKey() {
        String mac = getMacAddress();
        if (mac == null) {
            mac = getFallbackId();
        }

        if (mac.length() >= 16) {
            return mac.substring(0, 16);
        } else {
            return String.format("%-16s", mac).replace(' ', '0');
        }
    }

    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni == null || ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) sb.append(String.format("%02X", b));
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getFallbackId() {
        try {
            StringBuilder sb = new StringBuilder();

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                String cpu = System.getenv("PROCESSOR_IDENTIFIER");
                if (cpu != null) sb.append(cpu);
            } else {
                String machineId = readFile("/etc/machine-id");
                if (machineId != null) sb.append(machineId);
                else {
                    String cpu = readFile("/proc/cpuinfo");
                    if (cpu != null) sb.append(cpu.hashCode());
                }
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02X", b));
            return hex.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "DefaultSecretKey!";
        }
    }

    private static String readFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}