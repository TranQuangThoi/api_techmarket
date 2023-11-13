package com.techmarket.api.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "db_order")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Order extends Auditable<String>{
    @Id
    @GenericGenerator(name = "idGenerator", strategy = "com.techmarket.api.service.id.IdGenerator")
    @GeneratedValue(generator = "idGenerator")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = true)
    private User user;

    @Column(name = "total_money")
    private Double totalMoney =0.0;

    @Column(name = "payment_method")
    private Integer paymentMethod;

    @Column(columnDefinition = "text")
    private String note;
    private String address;
    private String province;
    private String ward;
    private String district;
    private String receiver;
    private String phone;
    @Column(name = "expected_Delivery_Date")
    private Date expectedDeliveryDate;
    @Column(name = "is_paid")
    private Boolean isPaid=false;
    @Column(name = "is_delivery")
    private Boolean isDelivery=false;
    private Integer state;

}