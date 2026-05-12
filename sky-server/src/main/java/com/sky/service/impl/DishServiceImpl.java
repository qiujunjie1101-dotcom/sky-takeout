package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishMapper;
import com.sky.mapper.FlavorMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private FlavorMapper favorMapper;

    @Override
    @Transactional  //保证方法是原子性
    public void saveWithFlavor(DishDTO dishDTO) {
        //dish对象就都是
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);

        //获取生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();

        // 将 插入菜品中的id 赋值给到 口味中的id
//        for (DishFlavor dishFlavor : flavors) {
//            dishFlavor.setId(dishId);
//        }
        flavors.forEach(flavor -> flavor.setDishId(dishId));


        if (flavors != null && !flavors.isEmpty()) {
             favorMapper.insertBatch(flavors);
        }

    }
}
