package com.github.prgrms.orders;

import com.github.prgrms.errors.NotFoundException;
import com.github.prgrms.products.Product;
import com.github.prgrms.products.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.google.common.base.Preconditions.checkArgument;
import static com.github.prgrms.utils.ObjectUtil.*;
import static com.github.prgrms.utils.NumberUtil.isBig;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class OrderService {

    private final ProductService productService;

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
  
  
    public OrderService(ProductService productService, ReviewRepository reviewRepository, OrderRepository orderRepository) {
      this.reviewRepository = reviewRepository;
      this.orderRepository = orderRepository;
      this.productService = productService;
    }
    
    @Transactional
    public ReviewDto review(Long orderId,String content){
      checkArgument(isNotEmpty(content), "content must be provided");
      checkArgument(!isBig(content.length(), 1000), "리뷰의 글자 수는 1000보다 클 수 없다.");

      OrderDto order = findOrderById(orderId);

      State state = order.getState();
      boolean isEquals = com.github.prgrms.utils.ObjectUtil.equals(state, State.COMPLETED);
      checkArgument(isEquals, 
      "Could not write review for order 1 because state(REQUESTED) is not allowed");    
    
      Long reviewId = order.getReviewId();
      boolean reviewCheck = reviewId == 0? true:false;
      checkArgument(reviewCheck, "Could not write review for order"+orderId+" because have already written");

      Long userId = order.getUserId();
      Long productId = order.getProductId();
      reviewId = new Long (saveReview(userId,productId,content));
      order.setReviewId(reviewId);
      updateOrderReviewSeq(order);

      Product product = productService.findById(productId)
      .orElseThrow(() -> new NotFoundException("Could not found product for " + productId));

      product.addReviewCount();
      productService.updateProduct(product);
      
      ReviewDto review = findReviewById(reviewId).map(ReviewDto::new).orElse(null);

      return review;
    
    }

    @Transactional(readOnly = true)
    public OrderDto findOrderById(Long orderId){
      checkNotNull(orderId,"orderId must be procided");

      Optional<Order> temp = orderRepository.findById(orderId);
      OrderDto order = temp.map(OrderDto::new).orElseThrow(() -> new NotFoundException("Could not found order for" + orderId));
      
      order.setReview(findReviewById(order.getReviewId()).orElse(null));

      return order;
    }

    @Transactional(readOnly = true)
    public Optional<Review> findReviewById(Long reviewId){
      checkNotNull(reviewId, "reviewId must be provided");
      return reviewRepository.findById(reviewId);
    }
    
    @Transactional
    public long saveReview(long userId,long productId, String content){
    Review review = new Review(null,userId,productId, content, null);
  
      return reviewRepository.save(review);
    }

    @Transactional
    public void updateOrderReviewSeq(OrderDto order){
      orderRepository.updateReviewSeq(order);
    }  


}