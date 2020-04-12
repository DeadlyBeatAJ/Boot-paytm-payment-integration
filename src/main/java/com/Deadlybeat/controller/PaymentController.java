package com.Deadlybeat.controller;

import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.Deadlybeat.model.PaytmDetails;
import com.paytm.pg.merchant.CheckSumServiceHelper;

@Controller
public class PaymentController {

	@Autowired
	private PaytmDetails paytmdetails;
	
	
	//to the the mobile and email form app.prop files
	@Autowired
	private Environment env;
	
	//return the home.html page
	@GetMapping
	public String home() {
		return "home";
	}
	
	
	// this will redirect our home page to paytm gateway
	@PostMapping("/pgredirect")
	public ModelAndView getRedirect(@RequestParam(name = "CUST_ID") String customerId,
            @RequestParam(name = "TXN_AMOUNT") String transactionAmount,
            @RequestParam(name = "ORDER_ID") String orderId) throws Exception {

		//we are redirecting to paytm url we set in App.yml from this controller using redirect
			ModelAndView modelAndView = new ModelAndView("redirect:" + paytmdetails.getPaytmUrl());
			TreeMap<String, String> parameters = new TreeMap<>();
			paytmdetails.getDetails().forEach((k, v) -> parameters.put(k, v));
			//will be taken from app.prop file
			parameters.put("MOBILE_NO", env.getProperty("paytm.mobile"));
			parameters.put("EMAIL", env.getProperty("paytm.email"));
			//from user form home.html
			parameters.put("ORDER_ID", orderId);
			parameters.put("TXN_AMOUNT", transactionAmount);
			parameters.put("CUST_ID", customerId);
			
			//generate checksumn so we added paytmchecksum jar in build path
			String checkSum = getCheckSum(parameters);
			parameters.put("CHECKSUMHASH", checkSum);
			
			modelAndView.addAllObjects(parameters);
			return modelAndView;
	}

	//created so the before request and after response checksum cab be validate for successful payment
	private String getCheckSum(TreeMap<String, String> parameters) throws Exception {
		// TODO Auto-generated method stub
//		return CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(paytmdetails.getMerchantId(), parameters);
		return CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(paytmdetails.getMerchantId(), parameters);
	}
	
	/*
	 * once the response come from paytm gateway we willl redirect it to response in
	 * our application i.e. /pgresponse this will be same as app.yml callbackUrl and
	 * it will return the report.html page
	 */
	@PostMapping(value = "/pgresponse")
    public String getResponseRedirect(HttpServletRequest request, Model model) {

        Map<String, String[]> mapData = request.getParameterMap();
        TreeMap<String, String> parameters = new TreeMap<String, String>();
        mapData.forEach((key, val) -> parameters.put(key, val[0]));
        String paytmChecksum = "";
        if (mapData.containsKey("CHECKSUMHASH")) {
            paytmChecksum = mapData.get("CHECKSUMHASH")[0];
        }
        String result;

        boolean isValideChecksum = false;
        System.out.println("RESULT : "+parameters.toString());
        try {
            isValideChecksum = validateCheckSum(parameters, paytmChecksum);
            //respcode to check payment failed or success
            if (isValideChecksum && parameters.containsKey("RESPCODE")) {
                if (parameters.get("RESPCODE").equals("01")) { 
                    result = "Payment Successful";
                } 
                else 
                {
                    result = "Payment Failed";
                }
            } 
            else 
            {
                result = "Checksum mismatched";
            }
        } catch (Exception e) {
            result = e.toString();
        }
        model.addAttribute("result",result); //capture the payment result failed or success
        parameters.remove("CHECKSUMHASH");
        model.addAttribute("parameters",parameters);
        return "report"; //return a hmtl file report.html
    }

	//validate the checksum
    private boolean validateCheckSum(TreeMap<String, String> parameters, String paytmChecksum) throws Exception {
        return CheckSumServiceHelper.getCheckSumServiceHelper().verifycheckSum(paytmdetails.getMerchantKey(),
                parameters, paytmChecksum);
    }	
	
	
}
