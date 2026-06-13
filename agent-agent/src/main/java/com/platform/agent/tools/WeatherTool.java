package com.platform.agent.tools;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Random;

@Component("weatherTool")
public class WeatherTool {
    public String execute(Map<?,?> args) {
        String city = String.valueOf(args.get("city"));
        int t = 15 + new Random().nextInt(20);
        return city + " 当前气温 " + t + "℃,晴";
    }
}
