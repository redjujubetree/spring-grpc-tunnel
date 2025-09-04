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

                   if (!isValidIpAddress(ip)) {
                      log.debug("skip invalid IP: {} (net interface: {})", ip, netIf.getName());
                      continue;
                   }
                   
                   if (!isPrivateIp(ip)) {
                      log.info("public IP: {} (net interface: {})", ip, netIf.getName());
                      if (isRealNetworkInterface(netIf) && bestPublicIp == null) {
                         bestPublicIp = ip;
                      }
                   } else {
                      log.debug("private IP: {} (net interface: {})", ip, netIf.getName());
                      if (localIp == null && isRealNetworkInterface(netIf)) {
                         localIp = ip;
                      }
                   }
                }
             }
          }

          String result = bestPublicIp != null ? bestPublicIp : 
                         (localIp != null ? localIp : InetAddress.getLocalHost().getHostAddress());
          log.info("the return IP: {}", result);
          return result;
          
       } catch (Exception e) {
          log.error("Error getting best IPv4 address", e);
          return null;
       }
    }

    /**
     * check if the given IP address is valid (not a placeholder or invalid address)
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        if (ip.startsWith("0.") ||
            ip.startsWith("1.0.0.") ||
            ip.startsWith("2.0.0.") ||
            ip.startsWith("3.0.0.") ||
            ip.equals("0.0.0.0") ||
            ip.equals("255.255.255.255")) {
            return false;
        }

        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * check if the network interface is a real physical interface
     */
    private static boolean isRealNetworkInterface(NetworkInterface netIf) {
        try {
            String name = netIf.getName().toLowerCase();
            String displayName = netIf.getDisplayName() != null ? 
                                netIf.getDisplayName().toLowerCase() : "";
            
            String[] virtualPatterns = {
                "docker", "veth", "br-", "vmnet", "vbox", "tun", "tap",
                "lo", "dummy", "bond", "team", "bridge", "virtual"
            };
            
            for (String pattern : virtualPatterns) {
                if (name.contains(pattern) || displayName.contains(pattern)) {
                    return false;
                }
            }
            
            byte[] mac = netIf.getHardwareAddress();
            if (mac == null || mac.length == 0) {
                return false;
            }
            
            boolean allZero = true;
            for (byte b : mac) {
                if (b != 0) {
                    allZero = false;
                    break;
                }
            }
            
            return !allZero;
            
        } catch (Exception e) {
            log.debug("check physical network interface failed: {}", netIf.getName(), e);
            return true;
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