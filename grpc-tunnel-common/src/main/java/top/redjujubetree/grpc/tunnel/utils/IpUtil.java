package top.redjujubetree.grpc.tunnel.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Slf4j
public class IpUtil {

    public static String getBestIpv4(){
       try {
          String localIp = null;
          String bestPublicIp = null;
          
          Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

          while (nets.hasMoreElements()) {
             NetworkInterface netIf = nets.nextElement();
             if (netIf.isLoopback() || !netIf.isUp()) {
                continue;
             }
             
             Enumeration<InetAddress> addrs = netIf.getInetAddresses();
             while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                   String ip = addr.getHostAddress();

                   // 跳过无效或占位符 IP
                   if (!isValidIpAddress(ip)) {
                      log.debug("跳过无效 IP: {} (接口: {})", ip, netIf.getName());
                      continue;
                   }
                   
                   if (!isPrivateIp(ip)) {
                      log.info("找到有效公网 IP: {} (接口: {})", ip, netIf.getName());
                      // 优先选择真实的网络接口
                      if (isRealNetworkInterface(netIf) && bestPublicIp == null) {
                         bestPublicIp = ip;
                      }
                   } else {
                      log.debug("找到私有 IP: {} (接口: {})", ip, netIf.getName());
                      if (localIp == null && isRealNetworkInterface(netIf)) {
                         localIp = ip;
                      }
                   }
                }
             }
          }

          String result = bestPublicIp != null ? bestPublicIp : 
                         (localIp != null ? localIp : InetAddress.getLocalHost().getHostAddress());
          log.info("最终选择的 IP: {}", result);
          return result;
          
       } catch (Exception e) {
          log.error("Error getting best IPv4 address", e);
          return null;
       }
    }

    /**
     * 检查 IP 地址是否有效（排除占位符和无效地址）
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 排除明显的占位符地址
        if (ip.startsWith("0.") ||
            ip.startsWith("1.0.0.") ||
            ip.startsWith("2.0.0.") ||
            ip.startsWith("3.0.0.") ||
            ip.equals("0.0.0.0") ||
            ip.equals("255.255.255.255")) {
            return false;
        }
        
        // 检查 IP 格式有效性
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为真实的物理网络接口
     */
    private static boolean isRealNetworkInterface(NetworkInterface netIf) {
        try {
            String name = netIf.getName().toLowerCase();
            String displayName = netIf.getDisplayName() != null ? 
                                netIf.getDisplayName().toLowerCase() : "";
            
            // 排除虚拟接口
            String[] virtualPatterns = {
                "docker", "veth", "br-", "vmnet", "vbox", "tun", "tap",
                "lo", "dummy", "bond", "team", "bridge", "virtual"
            };
            
            for (String pattern : virtualPatterns) {
                if (name.contains(pattern) || displayName.contains(pattern)) {
                    return false;
                }
            }
            
            // 检查是否有有效的 MAC 地址
            byte[] mac = netIf.getHardwareAddress();
            if (mac == null || mac.length == 0) {
                return false;
            }
            
            // 检查 MAC 地址是否全为 0（虚拟接口特征）
            boolean allZero = true;
            for (byte b : mac) {
                if (b != 0) {
                    allZero = false;
                    break;
                }
            }
            
            return !allZero;
            
        } catch (Exception e) {
            log.debug("无法检查网络接口: {}", netIf.getName(), e);
            return true; // 保守地认为是有效接口
        }
    }

    public static boolean isPrivateIp(String ip) {
       return ip.startsWith("10.") ||
             ip.startsWith("192.168.") ||
             (ip.startsWith("172.") && inRange(ip, 16, 31));
    }

    private static boolean inRange(String ip, int start, int end) {
       try {
          int second = Integer.parseInt(ip.split("\\.")[1]);
          return second >= start && second <= end;
       } catch (Exception e) {
          return false;
       }
    }
}