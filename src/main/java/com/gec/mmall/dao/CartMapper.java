package com.gec.mmall.dao;

import com.gec.mmall.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

	Cart selectCartByUserIdProductId(@Param("userId") Integer userId, @Param("productId")Integer productId);

    List<Cart> selectCartByUserId(Integer userId);

    //检查该用户购物车未选中的商品数量
    int selectCartProductCheckStatusByUserId(Integer userId);

    int deleteByUserIdProductIds(@Param("userId") Integer userId, @Param("productIdList") List<String> productIdList);

    //单选或反选(全选或全反选)
    int checkedOrUnCheckedProduct(@Param("userId")Integer userId,@Param("productId")Integer productId,@Param("checked") Integer checked);

    //获取购物车商品数量
    int selectCartProductCount(Integer userId);

	List<Cart> selectCheckedCartByUserId(Integer userId);
}