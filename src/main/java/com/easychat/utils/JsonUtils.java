package com.easychat.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.easychat.entity.ResultVo;
import com.easychat.hander.GlobalExceptionHandler;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.util.List;


public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    public static final SerializerFeature[] FEATURES = new SerializerFeature[]{SerializerFeature.WriteMapNullValue};

    public static String convertObjToJson(Object obj) {
        return JSON.toJSONString(obj, FEATURES);
    }

    public static <T> T convertJsonToObj(String json, Class<T> classz) {
        try {
            return JSONObject.parseObject(json, classz);
        } catch (Exception e) {
            logger.error("convertJsonToObj异常，json: {}", json, e);
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.JSON_ENTITY_FAILED);
        }
    }

    public static <T> List<T> convertJsonArrayToList(String json, Class<T> classz) {
        try {
            return JSONArray.parseArray(json, classz);
        } catch (Exception e) {
            logger.error("convertJsonArrayToList异常，json: {}", json, e);
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.JSON_TO_LIST_FAILED);
        }
    }
}
