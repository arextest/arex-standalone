package io.arex.standalone.common.util;

import io.arex.standalone.common.model.MockCategory;
import io.arex.standalone.common.model.Mocker;

import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommonUtils {

    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    public static String decode(String str) {
        try {
            return new String(Base64.getDecoder().decode(str));
        } catch (Exception e) {
            return str;
        }
    }

    public static String generateMockerJson(Mocker mocker) {
        Mocker.Target targetRequest = mocker.getTargetRequest();
        MockCategory category = mocker.getCategoryType();
        if (MockCategory.DYNAMIC_CLASS.getName().equals(category.getName())) {
            return "";
        }
        String response = mocker.getTargetResponse().getBody();
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        if (category.isEntryPoint()) {
            builder.append("\"operation\": \"").append(mocker.getOperationName()).append("\",");
            builder.append("\"request\": ").append(StringUtil.defaultString(
                    CommonUtils.decode(targetRequest.getBody()), "\"\"")).append(",");
            builder.append("\"response\": ").append(response);
        }
        if (MockCategory.DATABASE.getName().equals(category.getName())) {
//            compareMap.put("dbname", targetRequest.attributeAsString("dbName"));
            builder.append("\"sql\": \"").append(targetRequest.getBody()).append("\",");
            builder.append("\"result\": ").append(response);
        }
        if (MockCategory.HTTP_CLIENT.getName().equals(category.getName())) {
            builder.append("\"operation\": \"").append(mocker.getOperationName()).append("\",");
            builder.append("\"request\": \"").append(CommonUtils.decode(targetRequest.getBody())).append("\"");
        }
        if (MockCategory.REDIS.getName().equals(category.getName())) {
            builder.append("\"clusterName\": \"").append(targetRequest.attributeAsString("clusterName")).append("\",");
            builder.append("\"key\": ").append(targetRequest.getBody()).append(",");
            builder.append("\"result\": ").append(response);
        }
        builder.append("}");
        return builder.toString();
    }


}
