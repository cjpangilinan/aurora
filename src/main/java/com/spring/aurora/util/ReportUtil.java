package com.spring.aurora.util;

import java.util.List;

import com.spring.aurora.model.Expense;
import com.spring.aurora.model.Order;
import com.spring.aurora.model.Payment;

public class ReportUtil {

	public static Double getExpenseTotal (List<Expense> expenseList) {
		
		Double expenseTotal = 0.0;
		
		for (Expense e : expenseList) {
			expenseTotal += e.getAmount();
		}
		
		return expenseTotal;
	}
	
	public static Double getPaymentTotal (List<Payment> paymentList) {

		Double paymentTotal = 0.0;
		
		for (Payment p : paymentList) {
			paymentTotal += p.getAmount();
		}
		
		return paymentTotal;
	}
	
	public static int getRoundDeliveredTotal (List<Order> orderList) {
		
		int roundTotal = 0;
		
		for (Order o : orderList) {
			roundTotal += o.getRoundCount();
		}
		
		return roundTotal;
	}
	
	public static int getSlimDeliveredTotal (List<Order> orderList) {
		
		int slimTotal = 0;
		
		for (Order o : orderList) {
			slimTotal += o.getRoundCount();
		}
		
		return slimTotal;
	}
	
	public static boolean isLeapYear(int year) {

		if ((year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0))) {
			return true;
		} else {
			return false;
		}
	}
}