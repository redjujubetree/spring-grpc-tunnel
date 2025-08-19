package top.redjujubetree.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class PbTenderTvaluationCenter {
	private Long id;
	private String clientId;
	private String serverMachineName;
	private Date onlineTime;
	private Date offlineTime;
	private Boolean isDeleted;
	private Date createTime;
	private Date updateTime;

}
