package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boylu.common.Constants;
import com.boylu.entity.SysMenu;
import com.boylu.enums.MenuTypeEnum;
import com.boylu.mapper.SysMenuMapper;
import com.boylu.service.SysMenuService;
import com.boylu.vo.menu.RouterVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getMenuTree() {
        // 获取所有菜单
        List<SysMenu> menus = list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
        
        // 构建树形结构
        Map<Integer, List<SysMenu>> childrenMap = menus.stream()
                .filter(menu -> menu.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        menus.forEach(menu -> menu.setChildren(childrenMap.get(menu.getId())));

        return menus.stream()
                .filter(menu -> menu.getParentId() == 0)
                .collect(Collectors.toList());
    }


    @Override
    public void addMenu(SysMenu menu) {
        if (menu.getType().equals(MenuTypeEnum.CATALOG)) {
            menu.setComponent("Layout");
        }
        save(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        if (menu.getType().equals(MenuTypeEnum.CATALOG)) {
            menu.setComponent("Layout");
        }
        updateById(menu);
    }

    @Override
    public void deleteMenu(Integer id) {
        // 检查是否有子菜单
        if (count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0) {
            throw new RuntimeException("存在子菜单，不能删除");
        }
        removeById(id);
    }

    @Override
    public List<RouterVO> getCurrentUserMenu() {

        List<SysMenu> menus;
        if (StpUtil.hasRole(Constants.ADMIN)) {
            menus = baseMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .ne(SysMenu::getType,MenuTypeEnum.BUTTON.getCode()));
            ensureResourceApprovalMenu(menus);
        }else {
            menus = baseMapper.getMenusByUserId(StpUtil.getLoginIdAsInt(),MenuTypeEnum.BUTTON.getCode());
        }

        return this.buildRouterTree(menus);
    }


    public List<RouterVO> buildRouterTree(List<SysMenu> menus) {
        List<RouterVO> resultList = new ArrayList<>();
        for (SysMenu menu : menus) {
            Integer parentId = menu.getParentId();
            if ( parentId == null || parentId == 0) {
                RouterVO.MetaVO metaVO = new RouterVO.MetaVO(menu.getTitle(),menu.getIcon(),menu.getHidden(),menu.getIsExternal());
                RouterVO build = RouterVO.builder().id(menu.getId()).path(menu.getPath()).redirect(menu.getRedirect()).name(menu.getName()).component(menu.getComponent())
                        .meta(metaVO).sort(menu.getSort()).build();
                resultList.add(build);
            }
        }
        resultList.sort(Comparator.comparingInt(RouterVO::getSort));

        for (RouterVO routerVO : resultList) {
            routerVO.setChildren(getRouterChild(routerVO.getId(),menus));
        }
        return resultList;
    }

    private List<RouterVO> getRouterChild(Integer pid , List<SysMenu> menus){
        if (menus == null) {
            return Collections.emptyList();
        }
        Map<Integer, RouterVO> routerMap = new HashMap<>();
        for (SysMenu e: menus) {
            Integer parentId = e.getParentId();
            if(parentId != null && parentId.equals(pid)){
                // 子菜单的下级菜单
                RouterVO.MetaVO metaVO = new RouterVO.MetaVO(e.getTitle(),e.getIcon(),e.getHidden(),e.getIsExternal());
                RouterVO routerVO = RouterVO.builder()
                        .id(e.getId())
                        .path(e.getPath())
                        .redirect(e.getRedirect())
                        .name(e.getName())
                        .component(e.getComponent())
                        .meta(metaVO).sort(e.getSort()).build();

                if (StringUtils.isEmpty(e.getComponent()) && isParentView(e)){
                    routerVO.setComponent(Constants.PARENT_VIEW);
                }
                routerMap.put(e.getId(), routerVO);
            }
        }

        List<RouterVO> childrens = new ArrayList<>(routerMap.values());
        childrens.sort(Comparator.comparingInt(RouterVO::getSort));

        for (RouterVO e : childrens) {
            e.setChildren(getRouterChild(e.getId(), menus));
        }

        return childrens.isEmpty() ? Collections.emptyList() : childrens;
    }

    /**
     * 是否为parent_view组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(SysMenu menu)
    {
        return menu.getParentId() != 0 && MenuTypeEnum.MENU.equals(menu.getType());
    }

    private void ensureResourceApprovalMenu(List<SysMenu> menus) {
        if (menus == null || hasMenuComponent(menus, "/site/resource/index")) {
            return;
        }

        SysMenu siteMenu = findSiteMenu(menus);
        if (siteMenu == null) {
            siteMenu = new SysMenu();
            siteMenu.setId(-9000);
            siteMenu.setParentId(0);
            siteMenu.setPath("/site");
            siteMenu.setComponent("Layout");
            siteMenu.setTitle("站点管理");
            siteMenu.setSort(2);
            siteMenu.setIcon("DeleteLocation");
            siteMenu.setType(MenuTypeEnum.CATALOG);
            siteMenu.setRedirect("/site/resource");
            siteMenu.setHidden(0);
            siteMenu.setIsExternal(0);
            menus.add(siteMenu);
        }

        SysMenu resourceMenu = new SysMenu();
        resourceMenu.setId(-9001);
        resourceMenu.setParentId(siteMenu.getId());
        resourceMenu.setPath("resource");
        resourceMenu.setComponent("/site/resource/index");
        resourceMenu.setTitle("资源审核");
        resourceMenu.setSort(5);
        resourceMenu.setIcon("FolderChecked");
        resourceMenu.setType(MenuTypeEnum.MENU);
        resourceMenu.setHidden(0);
        resourceMenu.setIsExternal(0);
        menus.add(resourceMenu);
    }

    private boolean hasMenuComponent(List<SysMenu> menus, String component) {
        for (SysMenu menu : menus) {
            if (menu != null && StringUtils.equals(menu.getComponent(), component)) {
                return true;
            }
        }
        return false;
    }

    private SysMenu findSiteMenu(List<SysMenu> menus) {
        for (SysMenu menu : menus) {
            if (menu == null) {
                continue;
            }
            if (StringUtils.equals(menu.getPath(), "/site") || StringUtils.equals(menu.getTitle(), "站点管理")) {
                return menu;
            }
        }
        return null;
    }
}
