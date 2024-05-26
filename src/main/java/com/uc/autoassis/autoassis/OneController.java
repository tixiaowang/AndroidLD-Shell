package com.uc.autoassis.autoassis;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
public class OneController {

    @RequestMapping("/auto/getDump")
    public Object getDump(@RequestBody Map<String, String> body) {
        String uid = body.get("uid");
        Map<String, Object> m = new HashMap<>();
        m.put("code", "200");
        m.put("data", Main.m);
        return m;
    }

    @RequestMapping("/auto/switchDump")
    public Object switchDumpToggle(@RequestBody Map<String, String> body) {
        String change = body.get("change");
        if (StringUtils.hasText(change)) {
            Main.runToggle = !Main.runToggle;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("code", "200");
        m.put("data", Main.runToggle);
        return m;
    }
}
