package com.bcsport.admin.service.sticker;

import com.bcsport.admin.config.ZplPrinterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class ZplPrintService {

    @Autowired
    private ZplPrinterConfig config;

    /**
     * 获取可用打印机列表
     */
    public List<ZplPrinterConfig.PrinterItem> getPrinters() {
        return config.getPrinters();
    }

    /**
     * 发送 ZPL 到指定打印机
     */
    public void sendZpl(String ip, int port, String zpl) {
        int timeout = config.getPrinter().getTimeout();
        log.info("Sending ZPL to {}:{}, {} bytes", ip, port, zpl.length());
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.setSoTimeout(timeout);
            OutputStream out = socket.getOutputStream();
            out.write(zpl.getBytes(StandardCharsets.UTF_8));
            out.flush();
            log.info("ZPL sent successfully");
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Printer connection refused: " + ip + ":" + port);
        } catch (java.net.SocketTimeoutException e) {
            throw new RuntimeException("Printer connection timeout: " + ip + ":" + port);
        } catch (Exception e) {
            throw new RuntimeException("Print failed: " + e.getMessage());
        }
    }

    /**
     * 发送 ZPL 到默认打印机（第一台）
     */
    public void sendZpl(String zpl) {
        List<ZplPrinterConfig.PrinterItem> printers = config.getPrinters();
        if (printers.isEmpty()) {
            throw new RuntimeException("No printers configured");
        }
        ZplPrinterConfig.PrinterItem printer = printers.get(0);
        sendZpl(printer.getIp(), printer.getPort(), zpl);
    }
}
