package com.example.personal_project.component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.example.personal_project.model.*;
import com.example.personal_project.service.AudienceService;
import com.example.personal_project.service.CampaignService;
import com.example.personal_project.service.MailService;
import com.example.personal_project.service.MailTemplateService;
import com.example.personal_project.service.impl.MailServerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MailConsumer {
    private final AmazonSQS amazonSQSClient;
    private final ObjectMapper objectMapper;
    private final AudienceService audienceService;
    private final MailService mailService;
    private final MailServerService mailServerService;
    private final MailTemplateService mailTemplateService;
    private final CampaignService campaignService;
    @Value("${aws.queueName}")
    private String queueName;
    @Value("${aws.queue.url}")
    private String queueURL;

    public MailConsumer(AmazonSQS amazonSQSClient, ObjectMapper objectMapper, AudienceService audienceService, MailService mailService, MailServerService mailServerService, MailTemplateService mailTemplateService, CampaignService campaignService) {
        this.amazonSQSClient = amazonSQSClient;
        this.objectMapper = objectMapper;
        this.audienceService = audienceService;
        this.mailService = mailService;
        this.mailServerService = mailServerService;
        this.mailTemplateService = mailTemplateService;
        this.campaignService = campaignService;
    }

    public void consumeCampaignLongPoll() throws JsonProcessingException {
        log.info("long polling start!");
        String queueUrl = amazonSQSClient.getQueueUrl(queueName).getQueueUrl();
        //use long polling with waiting time of 20 second
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(queueURL)
                .withWaitTimeSeconds(20);
        while (true) {
            //receive message
            for (Message message : amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages()) {
                // Check if the message is visible
                if (message.getAttributes().containsKey("ApproximateReceiveCount")) {
                    int receiveCount = Integer.parseInt(message.getAttributes().get("ApproximateReceiveCount"));
                    if (receiveCount > 1) {
                        log.info("Message received multiple times. Skipping processing.");
                        return;
                    }
                }
                log.info("Read Message from queue: {}", message.getBody());
                JsonNode jsonNode = objectMapper.readTree(message.getBody());
                if (jsonNode.has("id")) {
                    Campaign campaign = objectMapper.treeToValue(jsonNode, Campaign.class);
                    List<Audience> audiences = audienceService.getAllAudienceByCampaign(campaign);
                    MailTemplate mailTemplate = mailTemplateService.getTemplateByCampaign(campaign);
                    EmailCampaign emailCampaign = new EmailCampaign(campaign, mailTemplate, audiences);
                    try {
                        List<Mail> mails = mailServerService.sendBatchMails(emailCampaign);
                        campaignService.updateCampaignExecuteStatus(campaign);
                        log.info("batch insert mail record successfully with campaignID :" + campaign.getId());
                    } catch (Exception e) {
                        log.error("batch insert email record fail : " + e.getMessage());
                    }
                } else {
                    log.warn("Message does not contain 'id' key.");
                }
                amazonSQSClient.deleteMessage(queueUrl, message.getReceiptHandle());
            }
        }
    }
}
