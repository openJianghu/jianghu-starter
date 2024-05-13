//package org.jianghu.app;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.core.env.Environment;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//@Slf4j
//@EnableScheduling
//@SpringBootApplication
//@EnableTransactionManagement
//public class JiangHuApplication implements ApplicationRunner {
//
//	@Autowired
//	private Environment environment;
//
//    public static void main(String[] args) {
//        SpringApplication.run(JiangHuApplication.class, args);
//    }
//
//	@Override
//	public void run(ApplicationArguments args) throws Exception {
//		int port = Integer.parseInt(environment.getProperty("server.port", "8080"));
//		log.info("jianghu version: {}", "5.0.1");
//		log.info("jianghu started on http://127.0.0.1:{} ", port);
//	}
//}
