package com.example.shop.controller;

import com.example.shop.dto.CheckoutItemInput;
import com.example.shop.dto.CheckoutItemView;
import com.example.shop.entity.AppUser;
import com.example.shop.entity.CustomerOrder;
import com.example.shop.service.AccountService;
import com.example.shop.service.NotificationService;
import com.example.shop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private static final String CART_SESSION_KEY = "SHOP_CART";
    private static final String CHECKOUT_ITEMS_SESSION_KEY = "CHECKOUT_ITEMS";
    private static final String CHECKOUT_SOURCE_SESSION_KEY = "CHECKOUT_SOURCE";

    private final OrderService orderService;
    private final AccountService accountService;
    private final NotificationService notificationService;

    public OrderController(OrderService orderService,
                           AccountService accountService,
                           NotificationService notificationService) {
        this.orderService = orderService;
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @GetMapping("/checkout/buy-now")
    public String buyNowCheckout(@RequestParam("productId") Long productId,
                                 Authentication authentication,
                                 HttpSession session,
                                 Model model) {
        List<CheckoutItemInput> inputs = List.of(new CheckoutItemInput(productId, 1));
        session.setAttribute(CHECKOUT_ITEMS_SESSION_KEY, inputs);
        session.setAttribute(CHECKOUT_SOURCE_SESSION_KEY, "buy-now");
        return renderCheckout(authentication.getName(), inputs, model);
    }

    @PostMapping("/checkout/cart")
    public String cartCheckout(@RequestParam(name = "selectedProductIds", required = false) List<Long> selectedProductIds,
                               Authentication authentication,
                               HttpSession session,
                               Model model) {
        if (selectedProductIds == null || selectedProductIds.isEmpty()) {
            return "redirect:/cart?checkoutError";
        }

        Map<Long, Integer> cart = getCart(session);
        List<CheckoutItemInput> inputs = new ArrayList<>();
        for (Long productId : selectedProductIds) {
            Integer qty = cart.get(productId);
            if (qty != null && qty > 0) {
                inputs.add(new CheckoutItemInput(productId, qty));
            }
        }

        if (inputs.isEmpty()) {
            return "redirect:/cart?checkoutError";
        }

        session.setAttribute(CHECKOUT_ITEMS_SESSION_KEY, inputs);
        session.setAttribute(CHECKOUT_SOURCE_SESSION_KEY, "cart");
        return renderCheckout(authentication.getName(), inputs, model);
    }

    @PostMapping("/checkout/confirm")
    public String confirmOrder(@RequestParam("shippingAddress") String shippingAddress,
                               @RequestParam("phoneNumber") String phoneNumber,
                               Authentication authentication,
                               HttpSession session) {
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            return "redirect:/checkout/review?addressError";
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "redirect:/checkout/review?phoneError";
        }

        List<CheckoutItemInput> inputs = getCheckoutInputs(session);
        if (inputs.isEmpty()) {
            return "redirect:/cart?checkoutError";
        }

        List<Long> productIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        for (CheckoutItemInput input : inputs) {
            productIds.add(input.getProductId());
            quantities.add(input.getQuantity());
        }

        List<CheckoutItemView> items = orderService.buildCheckoutItems(productIds, quantities);
        CustomerOrder order = orderService.createOrder(authentication.getName(), phoneNumber, shippingAddress, items);
        notificationService.notifyAdminNewOrder(order.getId(), authentication.getName());

        String source = (String) session.getAttribute(CHECKOUT_SOURCE_SESSION_KEY);
        if ("cart".equals(source)) {
            Map<Long, Integer> cart = getCart(session);
            for (CheckoutItemInput input : inputs) {
                cart.remove(input.getProductId());
            }
            session.setAttribute(CART_SESSION_KEY, cart);
        }

        session.removeAttribute(CHECKOUT_ITEMS_SESSION_KEY);
        session.removeAttribute(CHECKOUT_SOURCE_SESSION_KEY);
        return "redirect:/account/orders?ordered";
    }

    @GetMapping("/checkout/review")
    public String reviewCheckout(Authentication authentication, HttpSession session, Model model) {
        List<CheckoutItemInput> inputs = getCheckoutInputs(session);
        if (inputs.isEmpty()) {
            return "redirect:/cart?checkoutError";
        }
        return renderCheckout(authentication.getName(), inputs, model);
    }

    private String renderCheckout(String username, List<CheckoutItemInput> inputs, Model model) {
        List<Long> productIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        for (CheckoutItemInput input : inputs) {
            productIds.add(input.getProductId());
            quantities.add(input.getQuantity());
        }

        List<CheckoutItemView> checkoutItems = orderService.buildCheckoutItems(productIds, quantities);
        BigDecimal total = checkoutItems.stream()
                .map(CheckoutItemView::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AppUser user = accountService.getByUsername(username);
        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("checkoutTotal", total);
        model.addAttribute("accountUser", user);
        return "products/checkout";
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<Long, Integer> casted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof Long id && entry.getValue() instanceof Integer qty && qty > 0) {
                    casted.put(id, qty);
                }
            }
            return casted;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<CheckoutItemInput> getCheckoutInputs(HttpSession session) {
        Object raw = session.getAttribute(CHECKOUT_ITEMS_SESSION_KEY);
        if (raw instanceof List<?> rawList) {
            List<CheckoutItemInput> inputs = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof CheckoutItemInput input && input.getProductId() != null && input.getQuantity() > 0) {
                    inputs.add(input);
                }
            }
            return inputs;
        }
        return List.of();
    }
}
