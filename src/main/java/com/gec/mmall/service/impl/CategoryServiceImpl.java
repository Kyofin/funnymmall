package com.gec.mmall.service.impl;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.dao.CategoryMapper;
import com.gec.mmall.pojo.Category;
import com.gec.mmall.service.ICategoryService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

	public static final Logger LOGGER = LoggerFactory.getLogger(CategoryServiceImpl.class);

	@Autowired
	private CategoryMapper categoryMapper;

	@Override
	public ServerResponse addCategory(String categoryName, Integer parentId){
		if (parentId == null || StringUtils.isBlank(categoryName)){
			return ServerResponse.createByErrorMessage("添加品类参数错误");
		}
		//填充品类数据
		Category category = new Category();
		category.setName(categoryName);
		category.setParentId(parentId);
		category.setStatus(false);
		//持久化品类数据
		int resultCount = categoryMapper.insert(category);
		if (resultCount > 0){
			return ServerResponse.createBySuccessMessage("添加品类成功");
		}
		return ServerResponse.createByErrorMessage("添加品类失败");

	}

	@Override
	public ServerResponse updateCategoryName(String categoryName, Integer categoryId){
		if (categoryId == null || StringUtils.isBlank(categoryName)){
			return ServerResponse.createByErrorMessage("更新品类参数错误");
		}
		//填充品类要更新的数据
		Category category = new Category();
		category.setId(categoryId);
		category.setName(categoryName);
		//持久化品类数据(只更新非空的属性)
		int resultCount = categoryMapper.updateByPrimaryKeySelective(category);
		if (resultCount > 0){
			return ServerResponse.createBySuccessMessage("更新品类名称成功");
		}
		return ServerResponse.createByErrorMessage("更新品类名称失败");
	}

	/**
	 * 查找该品类的平级子分类(不递归)
	 * @param categoryId
	 * @return
	 */
	@Override
	public ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId) {
		List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId); //todo 区分品类状态
		if (CollectionUtils.isEmpty(categoryList)){
			LOGGER.info("未找到当前分类的子分类");
		}
		return ServerResponse.createBySuccess(categoryList);
	}

	/**
	 * 递归查询本节点的id及孩子节点的id
	 * @param categoryId
	 * @return
	 */
	@Override
	public ServerResponse selectCategoryAndChildrenById(Integer categoryId) {
		Set<Category> categorySet = Sets.newHashSet();
		//调用递归算法，算出所有子节点
		findChildCategory(categorySet,categoryId);

		List<Integer> categoryIdList = Lists.newArrayList();
		if (categoryId != null){
			for (Category categoryItem : categorySet) {
				categoryIdList.add(categoryItem.getId());
			}
		}
		return ServerResponse.createBySuccess(categoryIdList);
	}

	/**
	 * 递归算法，算出子节点，自己调自己
	 */
	private Set<Category> findChildCategory(Set<Category> categorySet,Integer categoryId){
		//查找自己，添加自己
		Category category = categoryMapper.selectByPrimaryKey(categoryId); //todo 区分品类状态
		if (category != null){
			categorySet.add(category);
		}
		//查找子节点，递归算法一定要有一个退出条件
		//0->10000->100000
		List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);//todo 区分品类状态
		for (Category categoryItem : categoryList) {
			findChildCategory(categorySet,categoryItem.getId());
		}
		return categorySet;
	}
}
