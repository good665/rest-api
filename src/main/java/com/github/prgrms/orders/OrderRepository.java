package com.github.prgrms.orders;

import java.util.Optional;

public interface OrderRepository {
    
    Optional<Order> findById(long id);
    
    void updateReviewSeq(OrderDto order);

}
