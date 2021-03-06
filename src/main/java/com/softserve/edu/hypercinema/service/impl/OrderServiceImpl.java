package com.softserve.edu.hypercinema.service.impl;

import com.softserve.edu.hypercinema.entity.TicketEntity;
import com.softserve.edu.hypercinema.entity.UserEntity;
import com.softserve.edu.hypercinema.exception.AccessViolationException;
import com.softserve.edu.hypercinema.exception.OrderNotFoundException;
import com.softserve.edu.hypercinema.entity.OrderEntity;
import com.softserve.edu.hypercinema.repository.OrderRepository;
import com.softserve.edu.hypercinema.service.OrderService;
import com.softserve.edu.hypercinema.service.PaymentService;
import com.softserve.edu.hypercinema.service.TicketService;
import com.softserve.edu.hypercinema.service.UserService;
import com.softserve.edu.hypercinema.util.AuthUtil;
import com.softserve.edu.hypercinema.util.CodeConfig;
import com.softserve.edu.hypercinema.util.VoucherGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_NOT_FOUND_MESSAGE = "Could not find order with id=";
    //    private static final String ORDER_NOT_FOUND_BY_USER_MESSAGE = "Could not find order with user=";
    private static final String ACCESS_VIOLATION_MESSAGE = "You don't have permission to view this order";


    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private PaymentService paymentService;


    @Override
    public void createOrder(OrderEntity order, Principal principal) {
        log.info("Creating new Order...");
        UserEntity user = userService.getUser(principal);
        log.info("User = " + user.getEmail());
        List<TicketEntity> tickets = order.getTickets();
        order.setTickets(Collections.emptyList());
        order.setUser(user);
        order.setPended(true);
        order.setConfirmed(true);
        order.setOrderNumber(VoucherGenerator.generate(new CodeConfig(null,null,null,null,null)));
        order.setOrderDate(LocalDateTime.now());

        orderRepository.saveAndFlush(order);

//        paymentService.createPayment(order.getPayment());

        for (TicketEntity ticket : tickets) {
            ticket.setOrder(order);
            ticketService.createTicket(ticket);
        }
    }

    @Override
    public List<OrderEntity> getOrders(Authentication authentication) {
        if (!AuthUtil.isAdmin(authentication) || !AuthUtil.isManager(authentication)) {
            List<OrderEntity> orders = userService.getUser(authentication).getOrders();
            orders.forEach(e -> System.out.println(e.getTickets().toString()));
            return userService.getUser(authentication).getOrders();
        } else return orderRepository.findAll();
    }

    @Override
    public List<OrderEntity> getOrders(Principal principal) {
        return userService.getUser(principal).getOrders();
    }

    @Override
    public OrderEntity getOrder(Long id, Authentication authentication) {
        if (!AuthUtil.isAdmin(authentication) || !AuthUtil.isManager(authentication)) {
            UserEntity userEntity = userService.getUser(authentication);
            boolean hasOrder = userEntity.getOrders().stream().anyMatch(o -> o.getId().equals(id));
            if (!hasOrder) {
                throw new AccessViolationException(ACCESS_VIOLATION_MESSAGE);
            }
        }
        return orderRepository.findById(id).orElseThrow(() ->
                new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + id));
    }

    @Override
    public void updateOrder(OrderEntity orderEntity) {
    }

    @Override
    public void deleteOrder(Long id) {

//        OrderEntity order = orderRepository.findById(id).orElseThrow(() ->
//                new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + id));
//        UserEntity user = order.getUser();
//        user.getOrders().removeAll(order.getTickets());
//        userService.updateUser(user);

        orderRepository.deleteById(id);
    }

    @Override
    public void deleteOrder(OrderEntity orderEntity) {

        orderRepository.delete(orderEntity);
    }

    @Override
    public List<OrderEntity> getAllOrders (){
        return orderRepository.findAll();
    }

}
