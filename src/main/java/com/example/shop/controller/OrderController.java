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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
                                 @RequestParam("selectedColor") String selectedColor,
                                 @RequestParam("selectedSize") String selectedSize,
                                 Authentication authentication,
                                 HttpSession session,
                                 Model model) {
        List<CheckoutItemInput> inputs = List.of(new CheckoutItemInput(
                productId,
                clean(selectedColor),
                normalizeSize(selectedSize),
                1
        ));
        session.setAttribute(CHECKOUT_ITEMS_SESSION_KEY, inputs);
        session.setAttribute(CHECKOUT_SOURCE_SESSION_KEY, "buy-now");
        return renderCheckout(authentication.getName(), inputs, model);
    }

    @PostMapping("/checkout/cart")
    public String cartCheckout(@RequestParam(name = "selectedCartKeys", required = false) List<String> selectedCartKeys,
                               Authentication authentication,
                               HttpSession session,
                               Model model) {
        if (selectedCartKeys == null || selectedCartKeys.isEmpty()) {
            return "redirect:/cart?checkoutError";
        }

        Map<String, Integer> cart = getCart(session);
        List<CheckoutItemInput> inputs = new ArrayList<>();
        for (String key : selectedCartKeys) {
            Integer qty = cart.get(key);
            CartSelection selection = parseCartKey(key);
            if (qty != null && qty > 0 && selection != null) {
                inputs.add(new CheckoutItemInput(selection.productId(), selection.color(), selection.size(), qty));
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

        List<CheckoutItemView> items = orderService.buildCheckoutItems(inputs);
        CustomerOrder order = orderService.createOrder(authentication.getName(), phoneNumber, shippingAddress, items);
        notificationService.notifyAdminNewOrder(order.getId(), authentication.getName());

        String source = (String) session.getAttribute(CHECKOUT_SOURCE_SESSION_KEY);
        if ("cart".equals(source)) {
            Map<String, Integer> cart = getCart(session);
            for (CheckoutItemInput input : inputs) {
                cart.remove(buildCartKey(input.getProductId(), input.getColor(), input.getSize()));
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
        List<CheckoutItemView> checkoutItems = orderService.buildCheckoutItems(inputs);
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
    private Map<String, Integer> getCart(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, Integer> casted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof Integer qty && qty > 0) {
                    casted.put(key, qty);
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
                if (item instanceof CheckoutItemInput input
                        && input.getProductId() != null
                        && clean(input.getColor()) != null
                        && normalizeSize(input.getSize()) != null
                        && input.getQuantity() > 0) {
                    input.setColor(clean(input.getColor()));
                    input.setSize(normalizeSize(input.getSize()));
                    inputs.add(input);
                }
            }
            return inputs;
        }
        return List.of();
    }

    private String buildCartKey(Long productId, String color, String size) {
        return productId + "|" + urlEncode(color) + "|" + urlEncode(size);
    }

    private CartSelection parseCartKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String[] parts = key.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }

        try {
            Long productId = Long.parseLong(parts[0]);
            String color = clean(urlDecode(parts[1]));
            String size = normalizeSize(urlDecode(parts[2]));
            if (color == null || size == null) {
                return null;
            }
            return new CartSelection(productId, color, size);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSize(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ROOT);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record CartSelection(Long productId, String color, String size) {
    }
}
