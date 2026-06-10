package com.bcsport.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "zpl")
public class ZplPrinterConfig {

    private Printer printer = new Printer();
    private List<PrinterItem> printers = new ArrayList<>();

    @Data
    public static class Printer {
        private int timeout = 5000;
    }

    @Data
    public static class PrinterItem {
        private String name;
        private String ip;
        private int port = 9100;
        private String tagSize;
    }
}
