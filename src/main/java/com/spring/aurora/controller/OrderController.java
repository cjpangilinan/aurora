package com.spring.aurora.controller;

import com.spring.aurora.entity.DailySalesEntity;
import com.spring.aurora.entity.DailyTotalsEntity;
import com.spring.aurora.entity.OrderCustomerEntity;
import com.spring.aurora.model.*;
import com.spring.aurora.service.*;
import com.spring.aurora.util.OrderFormValidator;
import com.spring.aurora.util.ReportUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	@Autowired
	OrderFormValidator orderFormValidator;
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(orderFormValidator);
	}

	@Autowired
    private OrderService orderService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private ContainerService containerService;
    
    @Autowired
    private DebtService debtService;
    
    @Autowired
    private ExpenseService expenseService;
    
    @Autowired
    private PaymentService paymentService;

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

	@RequestMapping(value = "/neworder", method = RequestMethod.GET)
	public String newOrder(@RequestParam String customerId, Model model) {
		logger.debug("New Order form for customer: " + customerId);
		Order order = new Order();
		order.setCustomerId(customerId);
		model.addAttribute("orderForm", order);
		model.addAttribute("customerId", customerId);
		model.addAttribute("newDrNumber", orderService.getNewDrNumber());

		Customer customer = customerService.view(customerId);
		model.addAttribute("customerName", customer.getName());
		return "new-order";
	}

	@RequestMapping(value = "/daily", method = RequestMethod.GET)
    public String dailySales(Model model, @RequestParam(value="d", defaultValue="today", required=false) String d ) {
        logger.info("Daily sales report.");
        
        int totalSlimDelivered = 0;
        int totalRoundDelivered = 0;
        int totalSlimReturned = 0;
        int totalRoundReturned = 0;
        Double totalExpenses = 0.0;
        Double totalPayments = 0.0;
        Double totalDebt = 0.0;
        
        List<DailySalesEntity> dseList = new ArrayList<>();
        
        Date date = ("today".equalsIgnoreCase(d)) ? Date.valueOf(LocalDate.now()) : Date.valueOf(LocalDate.parse(d));
        String datePicked = new SimpleDateFormat("MMM dd YYYY").format(date);
        model.addAttribute("datePicked", datePicked);
        
        List<Order> orderList = orderService.findAllOrdersToday(date);
        
        for (Order o : orderList) {
        	if (o.getStatus().equalsIgnoreCase("Delivered")) {
        		DailySalesEntity dse = new DailySalesEntity();
            	dse.setOrder(o);
            	
            	Customer c = customerService.view(o.getCustomerId());
            	dse.setCustomerName(c.getName());
            	totalPayments += o.getAmountPaid();
            	dse.setPaidAmount(o.getAmountPaid());
            	
            	totalSlimDelivered += o.getSlimCount();
            	totalRoundDelivered += o.getRoundCount();
            	totalSlimReturned += o.getSlimReturned();
            	totalRoundReturned += o.getRoundReturned();
            	
            	Double debt = o.getTotalAmount() - o.getAmountPaid();
            	totalDebt += debt;
            	dse.setBalanceAmount(debt);
            	
            	Timestamp dateTime = o.getCreatedAt();
            	String formattedDate = new SimpleDateFormat("h:mm a").format(dateTime);
            	
            	dse.setRemarks(o.getRemarks());
            	dse.setDateAndTime(formattedDate);
            	dseList.add(dse);
        	}
        }
        
        List<Expense> expenseList = new ArrayList<>();
        expenseList = expenseService.findAllByDate(date);
        
        for (Expense e : expenseList) {
        	DailySalesEntity dse = new DailySalesEntity();
        	dse.setRemarks(e.getDescription());
        	totalExpenses += e.getAmount();
        	dse.setExpenseAmount(e.getAmount());
        	dseList.add(dse);
        }
        
        List<Payment> paymentList = new ArrayList<>();
        paymentList = paymentService.findAllByDate(date);
        
        for (Payment p : paymentList) {
        	DailySalesEntity dse = new DailySalesEntity();
        	dse.setCustomerName(customerService.view(p.getCustomerId()).getName());
        	dse.setRemarks(p.getRemarks());
        	totalPayments += p.getAmount();
        	dse.setPaidAmount(p.getAmount());
        	dseList.add(dse);
        }
        
        model.addAttribute("dailySales", dseList);
        
        model.addAttribute("totalSlimDelivered", totalSlimDelivered);
        model.addAttribute("totalRoundDelivered", totalRoundDelivered);
        model.addAttribute("totalSlimReturned", totalSlimReturned);
        model.addAttribute("totalRoundReturned", totalRoundReturned);
        
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalPayments", totalPayments);
        model.addAttribute("totalDebt", totalDebt);
        
        model.addAttribute("ar", totalPayments - totalExpenses);
        return "daily-sales";
    }
    
	@RequestMapping(value = "/monthly", method = RequestMethod.GET)
	public String monthlyTotals(Model model, @RequestParam(value = "m", required = false) String m,
			@RequestParam(value = "y", required = false) String y) {
		
		List<DailyTotalsEntity> dteList = new ArrayList<>();
		int days = 31;
		
		if (Integer.valueOf(m) % 2 == 0) {
			if (Integer.valueOf(m) == 2) {
				
				if (ReportUtil.isLeapYear(Integer.parseInt(y))) {
					days = 29;
				} else {
					days = 28;
				}
			} else {
				days = 30;
			}
		}
		
		for (int i = 1; i <= days; i++) {
//			String dateStr = y + "-" + m + "-" + i;
			
			DailyTotalsEntity dte = new DailyTotalsEntity();
			Double totalCash = 0.0;
			Double totalPayment = 0.0;
			Double totalExpense = 0.0;
			int totalRoundDelivered = 0;
			int totalSlimDelivered = 0;
			
			String day = "";
			String month = "";
			
			if (i<10) {
				day = "0" + i;
			} else {
				day = "" + i;
			}
			
			if (Integer.parseInt(m)<10) {
				month = "0" + m;
			} else {
				month = "" + m;
			}
			
			String dateStr = y + "-" + month + "-" + day;
			Date date = (Date.valueOf(LocalDate.parse(dateStr)));
			List<Order> orderList = orderService.findAllOrdersToday(date);
			System.out.println("Date: " + date + " : " + orderList.size());
			
			for (Order o : orderList) {
				totalCash += o.getAmountPaid();
			}
			
			totalPayment = ReportUtil.getPaymentTotal(paymentService.findAllByDate(date));
			totalExpense = ReportUtil.getExpenseTotal(expenseService.findAllByDate(date));
			totalRoundDelivered = ReportUtil.getRoundDeliveredTotal(orderList);
			totalSlimDelivered = ReportUtil.getSlimDeliveredTotal(orderList);

			dte.setDate(date);
			dte.setTotalCash(totalCash);
			dte.setTotalPayments(totalPayment);
			dte.setTotalExpenses(totalExpense);
			dte.setTotalDeliveredRound(totalRoundDelivered);
			dte.setTotalDeliveredSlim(totalSlimDelivered);
			dte.setTotalDeliveredContainers(totalRoundDelivered + totalSlimDelivered);
			
			dteList.add(dte);
		}
		
		model.addAttribute("dailyTotals", dteList);
    	return "monthly-totals";
    }
    
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String listOrders(Model model) {
    	logger.info("List all orders.");
        
    	List<Order> orderList = new ArrayList<>();
    	List<OrderCustomerEntity> orderCustomerEntityList = new ArrayList<>();
        
    	orderList = orderService.findAll();
        
        for (Order order : orderList) {
        	OrderCustomerEntity oce = new OrderCustomerEntity();
        	oce.setOrder(order);
        	Customer customer = customerService.view(order.getCustomerId());
            oce.setCustomerName(customer.getName());
            String formattedDate = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a").format(order.getCreatedAt());
            oce.setFormattedDate(formattedDate);
            orderCustomerEntityList.add(oce);
        }
        model.addAttribute("orders", orderCustomerEntityList);
        return "list-orders";
    }

    @RequestMapping("/{customerId}")
    public List<Order> findAllOrdersByCustomerId(@PathVariable String customerId) {
        List<Order> orders = new ArrayList<>();

        return orders;
    }

    @RequestMapping("/{customerId}/new")
    public Order saveOrderByCustomer(Order order) {
        return order;
    }
    
    @RequestMapping("/cancel")
	public String cancelOrder(Order order, BindingResult result, @RequestParam String orderId,
			final RedirectAttributes redirectAttributes) {

    	if (result.hasErrors()) {
            return "list-orders";
        } else {
        	orderService.cancelOrder(orderId);
        	redirectAttributes.addFlashAttribute("msg", "Order has been cancelled.");
        }
    	
    	return "redirect:/orders/list";
    }
    
    @RequestMapping("/deliver")
	public String deliverOrder(Order order, BindingResult result, @RequestParam String orderId,
			final RedirectAttributes redirectAttributes) {

    	if (result.hasErrors()) {
            return "list-orders";
        } else {
        	
        	order = orderService.findOrderByOrderId(orderId);
        	saveDebt(order.getAmountPaid(), order.getTotalAmount(), order.getCustomerId(), order.getOrderId());
        	saveContainerActivity(order.getSlimCount(), order.getRoundCount(), order.getSlimReturned(), order.getRoundReturned(), order.getCustomerId());
        	
        	orderService.setToDelivered(orderId);
        	redirectAttributes.addFlashAttribute("msg", "Order has been set to delivered.");
        }
    	
    	return "redirect:/orders/list";
    }
    
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String saveOrder(@ModelAttribute("orderForm") @Validated Order order,
                               BindingResult result, Model model,
                               final RedirectAttributes redirectAttributes) {
        logger.debug("Save order.");
        if (result.hasErrors()) {
            model.addAttribute("customerId", order.getCustomerId());
            model.addAttribute("newDrNumber", orderService.getNewDrNumber());

            Customer customer = customerService.view(order.getCustomerId());
            model.addAttribute("customerName", customer.getName());
            return "new-order";
        } else {
            // Add message to flash scope
            redirectAttributes.addFlashAttribute("css", "success");
            if(order.isNew()){
                redirectAttributes.addFlashAttribute("msg", "Order created successfully!");
            }else{
                redirectAttributes.addFlashAttribute("msg", "Order updated successfully!");
            }
            
            order.setDeliveryReceiptNum(orderService.getNewDrNumber());
            order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
            order.setStatus("Pending");
            
            orderService.insert(order);
            
            //saveDebt(order.getAmountPaid(), order.getTotalAmount(), order.getCustomerId(), order.getOrderId());
            
            // POST/REDIRECT/GET
            return "redirect:/orders/list";
            //return "redirect:/customers/" + customer.getCustomerId();
        }
    }
    
    /**
     * Saves the debt of the customer if amount paid is less than total amount for a given order.
     * 
     * @param amountPaid
     * @param totalAmount
     * @param customerId
     */
    public void saveDebt (double amountPaid, double totalAmount, String customerId, String orderId) {
    	
    	double deficit = totalAmount - amountPaid;
    	
    	if (deficit != 0.0) {
    		Debt debtEntry = new Debt();
    		debtEntry.setCustomerId(customerId);
    		debtEntry.setAmount(deficit);
    		
    		if (amountPaid == 0) {
    			debtEntry.setRemarks("Total amount is: Php" + totalAmount + " but the customer hasn't paid yet");
    		} else {
    			debtEntry.setRemarks("Total amount is: Php" + totalAmount + " but the amount paid is only Php" + amountPaid);
    		}
    		
    		
    		debtEntry.setCreatedAt(Date.valueOf(LocalDate.now()));
    		debtEntry.setOrderId(orderId);
    		debtService.insert(debtEntry);
    	}
    }
    
    /**
     * Saves the returned and borrowed containers into the Container table.
     * 
     * @param slimCount
     * @param roundCount
     * @param slimReturned
     * @param roundReturned
     * @param customerId
     */
    public void saveContainerActivity (int slimCount, int roundCount, int slimReturned, int roundReturned, String customerId) {
    	
    	Container containerActivity = new Container();
        containerActivity.setCustomerId(customerId);
        containerActivity.setRoundCount(roundCount);
        containerActivity.setSlimCount(slimCount);
        containerActivity.setStatus("B");
        containerActivity.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        containerService.insert(containerActivity);
        
        if (slimReturned != 0 || roundReturned != 0) {
        	containerActivity = new Container();
            containerActivity.setCustomerId(customerId);
            containerActivity.setRoundCount(roundReturned);
            containerActivity.setSlimCount(slimReturned);
            containerActivity.setStatus("R");
            containerActivity.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
            containerService.insert(containerActivity);
        }
    }
}
