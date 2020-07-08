package com.github.tinyurl.controller;

import com.github.tinyurl.constant.Constants;
import com.github.tinyurl.domain.Response;
import com.github.tinyurl.domain.request.GenerateRequest;
import com.github.tinyurl.service.TinyUrlService;
import com.github.tinyurl.util.ArrayUtil;
import com.github.tinyurl.util.MapUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * 短连接生成Controller
 *
 * @author errorfatal89@gmail.com
 * @date 2020/07/02
 */
@Controller("/")
public class TinyUrlController {
    @Resource
    private HttpServletRequest request;

    @Resource
    private TinyUrlService tinyUrlService;

    /**
     * 生成短连接
     * @param generateRequest 生成短连接请求
     * @return 短连接
     */
    @PostMapping("/generate")
    public Response generate(@Valid GenerateRequest generateRequest) {
        String tinyUrl = tinyUrlService.generate(generateRequest);
        return Response.success(tinyUrl);
    }

    /**
     * 重定向到原始URL
     * @param param 短连接参数
     */
    @GetMapping("/{key}")
    public String redirect(@PathVariable("key") String param) {
        // 处理短连接中携带参数场景
        String orgUrl = tinyUrlService.getRedirectUrl(param);
        StringBuilder finalUrl = new StringBuilder(orgUrl);
        if (finalUrl.charAt(finalUrl.length() - 1) == Constants.AMPERSAND) {
            finalUrl.deleteCharAt(finalUrl.length() - 1);
        }
        
        Map<String, String[]> parameterMap =request.getParameterMap();
        if (MapUtil.isNotEmpty(parameterMap)) {
            int index = orgUrl.indexOf(Constants.QUESTION_MARK);
            if (index != -1) {
                finalUrl.append(Constants.QUESTION_MARK);
            }

            parameterMap.forEach((key, values) -> {
                if (ArrayUtil.isNotEmpty(values)) {
                    for (String value : values) {
                        finalUrl.append(Constants.AMPERSAND).append(key).append('=').append(value);
                    }
                }
            });
        }

        return "redirect:" + finalUrl.toString();
    }
}