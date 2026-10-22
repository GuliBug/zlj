package com.zhenglongji.mall.mall.domain.dto;

import java.util.List;

public record StocktakeBody(List<StocktakeItemBody> items, String remark) {}
