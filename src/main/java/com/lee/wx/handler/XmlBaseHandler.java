package com.lee.wx.handler;

import com.google.gson.Gson;
import com.lee.retrofit.entity.BaseResponse;
import com.lee.utils.Constants;
import com.lee.utils.HttpUtils;
import com.lee.utils.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.Routed;
import org.json.JSONObject;
import org.json.XML;

import java.lang.reflect.Type;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class XmlBaseHandler<T> extends SimpleChannelInboundHandler<FullHttpRequest> {

    private int contentLength = 0;
    private int curContentLength;
    private StringBuilder allContent = new StringBuilder();

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpContent) {
            ByteBuf bytebuf = ((DefaultHttpContent) msg).content();
            byte[] contents = new byte[bytebuf.readableBytes()];
            bytebuf.readBytes(contents);
            curContentLength += contents.length;
            allContent.append(new String(contents));
            System.out.println("curContentLength is : " + curContentLength);
            if (curContentLength == contentLength) {
                System.out.println("all body is accept success \n content is : " + allContent.toString());
                JSONObject xmlJSONObject = XML.toJSONObject(allContent.toString());
                T t = getRequestEntity(xmlJSONObject.toString());
                handleRequest(ctx, t);
            }
        } else if (msg instanceof Routed) {
            HttpRequest httpRequest = ((Routed) msg).request();
            HttpHeaders headers = httpRequest.headers();
            contentLength = headers.getInt(HttpHeaderNames.CONTENT_LENGTH, 0);
            System.out.println("http request header is : " + headers.toString() + " http protocol is : "
                    + httpRequest.protocolVersion());
            final String uri = httpRequest.uri();
            System.out.println("http request uri is : " + uri +" \n content length is : "+contentLength);
        }
    }

    protected T getRequestEntity(String bodyContent) {
        System.out.println("request body string is : "+bodyContent);
        Gson gson = new Gson();
        Type type = getRequestContentGsonType();
        return gson.fromJson(bodyContent, type);
    }

    protected abstract Type getRequestContentGsonType();

    protected abstract void handleRequest(ChannelHandlerContext ctx, T t);

    protected void handleSuccessResponse(ChannelHandlerContext ctx, String message) {
        BaseResponse baseResponse = new BaseResponse(true, Constants.Status.STATUS_SUCCESS, message);
        String responseContent = Utils.getJsonString(baseResponse);
        System.out.println("response content is : " + responseContent);

        ByteBuf byteBuf = ctx.alloc().buffer(responseContent.length());
        byteBuf.writeBytes(responseContent.getBytes());
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        HttpUtils.addCommonHttpHeader(response, responseContent, 0, "");
        HttpUtils.addCacheHeader(response);
        ctx.writeAndFlush(response);
    }

    protected void handleFailResponse(ChannelHandlerContext ctx, String errorStatus, String message) {
        BaseResponse baseResponse = new BaseResponse(false, errorStatus, message);
        String responseContent = Utils.getJsonString(baseResponse);
        System.out.println("response content is : " + responseContent);

        ByteBuf byteBuf = ctx.alloc().buffer(responseContent.length());
        byteBuf.writeBytes(responseContent.getBytes());
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        HttpUtils.addCommonHttpHeader(response, responseContent, 0, "");
        HttpUtils.addCacheHeader(response);
        ctx.writeAndFlush(response);
    }

}