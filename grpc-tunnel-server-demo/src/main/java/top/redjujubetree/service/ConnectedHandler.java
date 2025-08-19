package top.redjujubetree.service;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import com.alibaba.fastjson2.JSON;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;
import top.redjujubetree.domain.entity.PbTenderTvaluationCenter;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ConnectedHandler implements MessageHandler {
	private Snowflake snowflake = new Snowflake();
	@Override
	public boolean supports(TunnelMessage message) {
		return message.hasRequest() && MessageType.CLIENT_REQUEST.equals(message.getType()) && "CONNECT".equals(message.getRequest().getType());
	}

	@Override
	public CompletableFuture<TunnelMessage> handle(TunnelMessage message) {
		return CompletableFuture.supplyAsync(() -> {
			PbTenderTvaluationCenter center = new PbTenderTvaluationCenter();
			center.setId(snowflake.nextId());
			center.setCreateTime(new Date());
			center.setClientId(message.getClientId());
			center.setOnlineTime(new Date());
			center.setIsDeleted(Boolean.FALSE);
			String mapStr = message.getRequest().getData().toStringUtf8();
			Map<String, String> map = JSON.parseObject(mapStr, Map.class);
			center.setServerMachineName(map.get("serverMachineName"));
			log.info(JSON.toJSONString(center));
			return null;
		});
	}

	@Override
	public int getOrder() {
		return MessageHandler.super.getOrder();
	}
}
