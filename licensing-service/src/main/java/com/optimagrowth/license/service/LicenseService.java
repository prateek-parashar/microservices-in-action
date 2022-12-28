package com.optimagrowth.license.service;

import com.optimagrowth.license.config.ServiceConfig;
import com.optimagrowth.license.model.License;
import com.optimagrowth.license.model.Organization;
import com.optimagrowth.license.repository.LicenseRepository;
import com.optimagrowth.license.service.client.OrganizationDiscoveryClient;
import com.optimagrowth.license.service.client.OrganizationFeignClient;
import com.optimagrowth.license.service.client.OrganizationRestTemplateClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;


@Service
public class LicenseService {

	@Autowired
	MessageSource messages;

	@Autowired
	private LicenseRepository licenseRepository;

	@Autowired
	ServiceConfig config;

	@Autowired
	OrganizationFeignClient organizationFeignClient;

	@Autowired
	OrganizationRestTemplateClient organizationRestClient;

	@Autowired
	OrganizationDiscoveryClient organizationDiscoveryClient;

	@Autowired
	CircuitBreakerRegistry registry;


	private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

	public License getLicense(String licenseId, String organizationId, String clientType){
		License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);
		if (null == license) {
			throw new IllegalArgumentException(String.format(messages.getMessage("license.search.error.message", null, null),licenseId, organizationId));	
		}

		Organization organization = retrieveOrganizationInfo(organizationId, clientType);
		if (null != organization) {
			license.setOrganizationName(organization.getName());
			license.setContactName(organization.getContactName());
			license.setContactEmail(organization.getContactEmail());
			license.setContactPhone(organization.getContactPhone());
		}

		return license.withComment(config.getProperty());
	}

	@CircuitBreaker(name = "organizationService")
	private Organization retrieveOrganizationInfo(String organizationId, String clientType) {
		Organization organization = null;
		organization = organizationRestClient.getOrganization(organizationId);

//		switch (clientType) {
//		case "feign":
//			System.out.println("I am using the feign client");
//			organization = organizationFeignClient.getOrganization(organizationId);
//			break;
//		case "rest":
//			System.out.println("I am using the rest client");
//			organization = organizationRestClient.getOrganization(organizationId);
//			break;
//		case "discovery":
//			System.out.println("I am using the discovery client");
//			organization = organizationDiscoveryClient.getOrganization(organizationId);
//			break;
//		default:
//			organization = organizationRestClient.getOrganization(organizationId);
//		}
//
		return organization;
	}

	public License createLicense(License license){
		license.setLicenseId(UUID.randomUUID().toString());
		licenseRepository.save(license);

		return license.withComment(config.getProperty());
	}

	public License updateLicense(License license){
		licenseRepository.save(license);

		return license.withComment(config.getProperty());
	}

	public String deleteLicense(String licenseId){
		String responseMessage = null;
		License license = new License();
		license.setLicenseId(licenseId);
		licenseRepository.delete(license);
		responseMessage = String.format(messages.getMessage("license.delete.message", null, null),licenseId);
		return responseMessage;

	}

	@CircuitBreaker(name = "licenseService")
	public List<License> getLicensesByOrganization(String organizationId) throws TimeoutException {
		logger.debug("**********************");
		System.out.println("***************************************");
		System.out.println(registry.circuitBreaker("licenseService").getState());
		System.out.println(registry.circuitBreaker("licenseService").getMetrics().getFailureRate());
		System.out.println(registry.circuitBreaker("licenseService").getMetrics().getNumberOfFailedCalls());
		System.out.println(registry.circuitBreaker("licenseService").getMetrics().getNumberOfSuccessfulCalls());
		System.out.println(registry.circuitBreaker("licenseService").getMetrics().getNumberOfBufferedCalls());
		System.out.println("**********************");
		randomlyRunLong();
		return licenseRepository.findByOrganizationId(organizationId);
	}

	private void randomlyRunLong() throws TimeoutException{
//		Random rand = new Random();
//		int randomNum = rand.nextInt(2) + 1;
		sleep();
	}
	private void sleep() throws TimeoutException {
		try {
			Thread.sleep(1000);
			throw new java.util.concurrent.TimeoutException();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}
}
