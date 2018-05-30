package com.gec.mmall.service;

import com.gec.mmall.common.ServerResponse;
import com.gec.mmall.pojo.Category;

import java.util.List;

public interface ICategoryService {
	ServerResponse addCategory(String categoryName, Integer parentId);

	ServerResponse updateCategoryName(String categoryName, Integer categoryId);

	ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId);

	ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId);
}
