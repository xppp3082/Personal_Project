package com.example.personal_project.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mail extends ReadObject {
    private Long id;
    @JsonProperty("campaign_id")
    private Long campaignID;
    @JsonProperty("company_id")
    private Long companyID;
    @JsonProperty("audience_id")
    private Long audienceID;
    private String recipientName;
    @JsonProperty("recipient_mail")
    private String recipientMail;
    private Timestamp timestamp;
    @JsonProperty("checktimes")
    private Integer checkTimes;
    @JsonProperty("mime_id")
    private String mimeID;
    //For checking webhook and spring already update the mail info
    private Boolean mailHookUpdate;
    private Boolean springBootUpdate;
}

