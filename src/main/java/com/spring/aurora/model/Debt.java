package com.spring.aurora.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

public class Debt {

	@Id
    @Column(name="customer_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	private String debtId;
	
	@Column(name="customer_id")
	private String customerId;
	
	@Column(name="amount")
	private Double amount;
	
	@Column(name="remarks")
	private String remarks;
	
	@Column(name="created_at")
	private LocalDateTime createdAt;

	public String getDebtId() {
		return debtId;
	}

	public void setDebtId(String debtId) {
		this.debtId = debtId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
}
