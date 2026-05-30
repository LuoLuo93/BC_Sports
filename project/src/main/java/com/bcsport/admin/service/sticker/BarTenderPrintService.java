package com.bcsport.admin.service.sticker;

import com.bcsport.admin.entity.sticker.StickerPrintOrderDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BarTenderPrintService {

    @Value("${bartender.enabled:false}")
    private boolean enabled;

    @Value("${bartender.url:}")
    private String url;

    private RestTemplate restTemplate;

    public boolean printOrder(String orderNo, List<StickerPrintOrderDetail> details) {
        if (!enabled) {
            log.warn("BarTender 打印未启用, 跳过打印, orderNo={}", orderNo);
            return false;
        }

        List<Map<String, Object>> printData = details.stream().map(d -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("styleNumber", d.getStyleNumber());
            item.put("materialName", d.getMaterialName());
            item.put("materialNumber", d.getMaterialNumber());
            item.put("brandName", d.getBrandName());
            item.put("sizeName", d.getSizeName());
            item.put("sizeGroup", d.getSizeGroup());
            item.put("ean13", d.getEan13());
            item.put("price", d.getPrice() != null ? d.getPrice().toPlainString() : "");
            item.put("printQty", d.getPrintQty());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("data", printData);

        try {
            RestTemplate rt = getRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> resp = rt.exchange(url, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("BarTender 打印请求成功, orderNo={}, 明细{}条", orderNo, details.size());
                return true;
            } else {
                log.error("BarTender 打印请求失败, orderNo={}, status={}", orderNo, resp.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("BarTender 打印请求异常, orderNo={}: {}", orderNo, e.getMessage(), e);
            return false;
        }
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }
}
