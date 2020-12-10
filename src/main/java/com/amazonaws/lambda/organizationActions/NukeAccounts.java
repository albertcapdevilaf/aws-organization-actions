package com.amazonaws.lambda.organizationActions;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.guardduty.AmazonGuardDuty;
import com.amazonaws.services.guardduty.AmazonGuardDutyClientBuilder;
import com.amazonaws.services.guardduty.model.DeleteDetectorRequest;
import com.amazonaws.services.guardduty.model.DeleteDetectorResult;
import com.amazonaws.services.guardduty.model.ListDetectorsRequest;
import com.amazonaws.services.guardduty.model.ListDetectorsResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.organizations.AWSOrganizationsClient;
import com.amazonaws.services.organizations.model.Account;
import com.amazonaws.services.organizations.model.ListAccountsRequest;
import com.amazonaws.services.organizations.model.ListAccountsResult;
import com.amazonaws.services.securityhub.AWSSecurityHub;
import com.amazonaws.services.securityhub.AWSSecurityHubClientBuilder;
import com.amazonaws.services.securityhub.model.DisableSecurityHubRequest;
import com.amazonaws.services.securityhub.model.DisableSecurityHubResult;
import com.amazonaws.services.securityhub.model.ResourceNotFoundException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;


public class NukeAccounts implements RequestHandler<NukeRequest, String> {

	/* Request example
	  { "region":"eu-west-1", "prefix":"workshop3", "skipGuardDuty":"false", "skipSecHub":"false" }
	 */
	
	@Override
	public String handleRequest(NukeRequest input, Context context) {
		
		//Param checks
		if (input.getRegion() == null)
			return "No region specified on the input";
		if (input.getPrefix() == null)
			return "No prefix specified on the input";
		try{
			Regions reg=Regions.fromName(input.getRegion());
		}
		catch (Exception e)
		{
			return "Invalid region: " + input.getRegion();
		}

		int accountsProcessed = 0;

		context.getLogger().log("\nSearching accounts with prefix '" + input.getPrefix() + "' in region " + input.getRegion());
		
		// Get accessor to the master account
		AWSCredentialsProvider awsCredProv = new DefaultAWSCredentialsProviderChain();
				
		// Get all the accounts of the Organization
		List<Account> accounts = getOrganizationAccounts(awsCredProv);
		
		// Nuke each account matching the prefix
		for (int i=0;i<accounts.size();i++)
		{
			Account account = accounts.get(i);
			if(account.getName().startsWith(input.getPrefix()))
			{
				accountsProcessed++;
				context.getLogger().log("\nProcessing the account " + account.getName());
				//Assume role
				AWSStaticCredentialsProvider staticcp = getAccountCredentials(awsCredProv, account.getId(),input.getRegion());
				
				// Disable Guard Duty
				if(input.getSkipGuardDuty()==null || input.getSkipGuardDuty()!=true)
				{
					if(disableGuardDuty(staticcp,input.getRegion()))
						context.getLogger().log("\n\tGuard Duty disabled");
					else
						context.getLogger().log("\n\tGuard Duty was already disabled");
				}

				// Disable Security Hub
				if(input.getSkipSecHub()==null || input.getSkipSecHub()!=true)
				{
					if(disableSecurityHub(staticcp,input.getRegion()))
						context.getLogger().log("\n\tSecurity Hub disabled");
					else
						context.getLogger().log("\n\tSecurity Hub was already disabled");
				}
			}
		}
		if(accountsProcessed == 0)
			return "No accounts found with the prefix " + input.getPrefix();
		else
			return accountsProcessed + " accounts processed";
	}

	// Returns all the accounts of the given organization
		private static List<Account> getOrganizationAccounts (AWSCredentialsProvider awsCredProv)
		{
			String nextToken = null;
			List<Account> accounts = new ArrayList<Account>();
			
			//get organization accounts
			ListAccountsResult laResult = null;
			ListAccountsRequest laReq = new ListAccountsRequest();
			laReq.setRequestCredentialsProvider(awsCredProv);

			AWSOrganizationsClient aoClient = new AWSOrganizationsClient(awsCredProv);

			do
			{
				laReq.setNextToken(nextToken);
				laResult = aoClient.listAccounts(laReq);
				accounts.addAll(laResult.getAccounts());
				nextToken = laResult.getNextToken();
			}
			while(nextToken!=null);
			
			return accounts;
		}
	
	// Role assumption for the account accountId
	private static AWSStaticCredentialsProvider getAccountCredentials(AWSCredentialsProvider credProv, String accountId, String region)
	{
		String roleARN = "arn:aws:iam::" + accountId + ":role/OrganizationAccountAccessRole";
		String roleSessionName = "NukeAccounts";

		// Creating the STS client is part of your trusted code. It has
		// the security credentials you use to obtain temporary security credentials.
		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
				.withCredentials(credProv)
				.withRegion(Regions.fromName(region))
				.build();

		// Assume the IAM role. 
		AssumeRoleRequest roleRequest = new AssumeRoleRequest()
				.withRoleArn(roleARN)
				.withRoleSessionName(roleSessionName);

		Credentials cred = stsClient.assumeRole(roleRequest).getCredentials();
				
		BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
				cred.getAccessKeyId(),
				cred.getSecretAccessKey(),
				cred.getSessionToken());
		AWSStaticCredentialsProvider staticcp = new AWSStaticCredentialsProvider(sessionCredentials);
		
		return (staticcp);

	}
	
	// Disables GuardDuty in a given region for an account.
	// Returns true if GuardDuty has been disabled, false if it was already disabled
	private static boolean disableGuardDuty(AWSStaticCredentialsProvider staticcp, String region)
	{
		AmazonGuardDuty guardduty = AmazonGuardDutyClientBuilder.standard()
				.withCredentials(staticcp)
				.withRegion(Regions.fromName(region))
				.build();

		ListDetectorsResult ldres = guardduty.listDetectors(new ListDetectorsRequest());

		if(ldres.getDetectorIds().size()==0)
			return false;

		DeleteDetectorRequest ddr = new DeleteDetectorRequest();
		ddr.setDetectorId(ldres.getDetectorIds().get(0));

		DeleteDetectorResult ddres = guardduty.deleteDetector(ddr);
		return true;
	}

	// Disables security hub in a given region for an account.
	// Returns true if SecurityHub has been disabled, false if it was already disabled
	private static boolean disableSecurityHub(AWSStaticCredentialsProvider staticcp, String region)
	{
		AWSSecurityHub sechub = AWSSecurityHubClientBuilder.standard()
				.withCredentials(staticcp)
				.withRegion(Regions.fromName(region))
				.build();

		try
		{
			DisableSecurityHubResult dshres = sechub.disableSecurityHub(new DisableSecurityHubRequest());
			return true;
		}
		catch(ResourceNotFoundException e)
		{
			return false;
		}
	}
	
	/*
	// Disables Macie in a given region for an account.
	// Returns true if Macie has been disabled, false if it was already disabled
	private static boolean disableMacie(AWSStaticCredentialsProvider staticcp, String region)
	{
		
		//AmazonMacieClientBuilder()
		
		AWSSecurityHub sechub = AWSSecurityHubClientBuilder.standard()
				.withCredentials(staticcp)
				.withRegion(Regions.fromName(region))
				.build();

		try
		{
			DisableSecurityHubResult dshres = sechub.disableSecurityHub(new DisableSecurityHubRequest());
			return true;
		}
		catch(ResourceNotFoundException e)
		{
			return false;
		}
	}*/
}
