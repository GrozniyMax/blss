package com.blss.blss.security;

import com.blss.blss.db.order.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Сервис проверки прав доступа к заказам.
 * Используется в @PreAuthorize для проверки доступа к конкретному заказу.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSecurityService {

    private final OrderRepo orderRepo;

    /**
     * Проверяет, имеет ли текущий пользователь доступ к заказу.
     *
     * Правила доступа (согласно {@link Role}):
     * <ul>
     *     <li>ADMIN, MANAGER — доступ ко всем заказам</li>
     *     <li>CONSULTANT — доступ ко всем заказам (выдача заказов)</li>
     *     <li>USER — доступ только к своим заказам</li>
     *     <li>WAREHOUSE — нет доступа к заказам</li>
     * </ul>
     *
     * @param orderId ID заказа
     * @param username имя текущего пользователя
     * @return true если доступ разрешён
     */
    public boolean canAccessOrder(UUID orderId, String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: not authenticated");
            return false;
        }

        // ADMIN, MANAGER, CONSULTANT — доступ ко всем заказам
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_CONSULTANT"))) {
            log.debug("Access granted: user {} has ADMIN/MANAGER/CONSULTANT role", username);
            return true;
        }

        // USER — доступ только к своим заказам
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
            var orderOpt = orderRepo.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.debug("Access denied: order {} not found", orderId);
                return false;
            }

            var order = orderOpt.get();
            boolean isOwner = username.equals(order.owner());

            if (isOwner) {
                log.debug("Access granted: user {} is owner of order {}", username, orderId);
            } else {
                log.debug("Access denied: user {} is not owner of order {}", username, orderId);
            }

            return isOwner;
        }

        log.debug("Access denied: user {} has no suitable role", username);
        return false;
    }

    /**
     * Проверяет, может ли текущий пользователь создать заказ от указанного владельца.
     *
     * Правила (согласно {@link Role}):
     * <ul>
     *     <li>ADMIN, MANAGER — могут создавать заказы от любого владельца</li>
     *     <li>USER — может создавать заказы только от своего имени</li>
     *     <li>CONSULTANT — не может создавать заказы (только выдача)</li>
     *     <li>WAREHOUSE — не может создавать заказы</li>
     * </ul>
     *
     * @param owner имя владельца заказа
     * @return true если создание разрешено
     */
    public boolean canCreateOrderFor(String owner) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Create denied: not authenticated");
            return false;
        }

        String currentUsername = authentication.getName();

        // ADMIN, MANAGER — могут создавать для любого
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"))) {
            log.debug("Create granted: user {} has ADMIN/MANAGER role", currentUsername);
            return true;
        }

        // USER — может создавать только для себя
        boolean isSelf = currentUsername.equals(owner);
        if (isSelf) {
            log.debug("Create granted: user {} creating order for self", currentUsername);
        } else {
            log.debug("Create denied: user {} cannot create order for {}", currentUsername, owner);
        }

        return isSelf;
    }
}
