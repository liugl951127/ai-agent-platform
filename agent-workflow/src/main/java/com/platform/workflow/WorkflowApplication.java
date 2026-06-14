package com.platform.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.platform.workflow","com.platform.common"})
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }

    @Bean
    public CommandLineRunner deploy(RepositoryService repo) {
        return args -> {
            String bpmn = "<?xml version='1.0' encoding='UTF-8'?>\n" +
"<definitions xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' " +
"xmlns:flowable='http://flowable.org/bpmn' targetNamespace='http://agent/platform'>\n" +
"  <process id='agentChat' name='智能体对话'>\n" +
"    <startEvent id='start'/>\n" +
"    <serviceTask id='rag' flowable:class='com.platform.workflow.delegate.RagDelegate'/>\n" +
"    <serviceTask id='llmCall' flowable:class='com.platform.workflow.delegate.LlmDelegate'/>\n" +
"    <exclusiveGateway id='gw'/>\n" +
"    <serviceTask id='toolCall' flowable:class='com.platform.workflow.delegate.ToolDelegate'/>\n" +
"    <endEvent id='end'/>\n" +
"    <sequenceFlow sourceRef='start' targetRef='rag'/>\n" +
"    <sequenceFlow sourceRef='rag' targetRef='llmCall'/>\n" +
"    <sequenceFlow sourceRef='llmCall' targetRef='gw'/>\n" +
"    <sequenceFlow sourceRef='gw' targetRef='toolCall'>\n" +
"      <conditionExpression>${needTool==true}</conditionExpression>\n" +
"    </sequenceFlow>\n" +
"    <sequenceFlow sourceRef='toolCall' targetRef='end'/>\n" +
"    <sequenceFlow sourceRef='gw' targetRef='end'/>\n" +
"  </process>\n" +
"</definitions>";
            repo.createDeployment().addString("agentChat.bpmn20.xml", bpmn)
               .name("agentChat").deploy();
        };
    }
}
