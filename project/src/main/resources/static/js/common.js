// 公共JavaScript函数

// 全局BC工具类
window.BC = {

    /**
     * HTML转义，防止XSS
     */
    escapeHtml: function(str) {
        if (str == null) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    },

    /**
     * 权限判断（需页面通过 th:inline 注入 window.userPermissions）
     */
    hasPermission: function(p) {
        if (!Array.isArray(window.userPermissions)) return false;
        return window.userPermissions.indexOf('*') > -1 || window.userPermissions.indexOf(p) > -1;
    },

    /**
     * 统一 AJAX 请求封装
     * 内置错误处理、code 校验、JSON 序列化
     * @param {Object} options 配置项
     * @param {String} options.url 请求地址
     * @param {String} [options.type='GET'] 请求方法
     * @param {Object} [options.data] 请求数据 (GET 时作 query params, 其他方法自动 JSON.stringify)
     * @param {Function} [options.success] 成功回调 (res) — 完整响应对象
     * @param {Function} [options.error] 错误回调 (errMessage) — 不传则自动 toast
     * @param {Boolean} [options.loading=false] 是否显示全局 loading
     */
    ajax: function(options) {
        var that = this;
        var opts = $.extend({
            type: 'GET',
            loading: false
        }, options);

        if (opts.loading) that.showLoading();

        var isBodyMethod = opts.type && opts.type !== 'GET';
        var ajaxConfig = {
            url: opts.url,
            type: opts.type,
            success: function(res) {
                if (opts.loading) that.hideLoading();
                if (res.code === 200) {
                    if (opts.success) opts.success(res.data !== undefined ? res.data : res);
                } else {
                    var msg = res.message || '操作失败';
                    if (opts.error) {
                        opts.error(msg);
                    } else {
                        that.toast(msg, 'error');
                    }
                }
            },
            error: function(xhr, status) {
                if (opts.loading) that.hideLoading();
                var msg = '网络请求失败';
                if (xhr.status === 401) {
                    msg = '登录已过期，请重新登录';
                } else if (xhr.status === 403) {
                    msg = '没有操作权限';
                }
                if (opts.error) {
                    opts.error(msg);
                } else {
                    that.toast(msg, 'error');
                }
            }
        };

        if (isBodyMethod && opts.data && typeof opts.data === 'object') {
            ajaxConfig.contentType = 'application/json';
            ajaxConfig.data = JSON.stringify(opts.data);
        } else {
            ajaxConfig.data = opts.data;
        }

        $.ajax(ajaxConfig);
    },

    showModal: function(selector) {
        var el = typeof selector === 'string' ? document.querySelector(selector) : selector;
        if (!el) return;
        var instance = bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el);
        instance.show();
    },

    hideModal: function(selector) {
        var el = typeof selector === 'string' ? document.querySelector(selector) : selector;
        if (!el) return;
        var instance = bootstrap.Modal.getInstance(el);
        if (instance) instance.hide();
    },

    /**
     * 渲染表格空数据状态
     * @param {String|jQuery} target tbody元素或选择器
     * @param {Number} cols 列数
     * @param {String} msg 提示文字
     */
    emptyState: function(target, cols, msg) {
        var tbody = typeof target === 'string' ? $(target) : target;
        if (!msg) msg = '暂无数据';
        tbody.html('<tr><td colspan="' + (cols || 10) + '" class="bc-empty-state"><i class="bi bi-inbox"></i><p>' + this.escapeHtml(msg) + '</p></td></tr>');
    },

    /**
     * 显示全局加载遮罩
     * @param {String} msg 提示文字
     */
    showLoading: function(msg) {
        // 统一使用 #global-loading，与 showLoadingOverlay 共享同一遮罩
        this.hideLoadingOverlay();
        var overlay = $(
            '<div id="global-loading" class="bc-global-loading">' +
            '  <div class="spinner-box">' +
            '    <div class="spinner-border text-primary" role="status"></div>' +
            '    <p class="loading-text">' + this.escapeHtml(msg || '加载中...') + '</p>' +
            '  </div>' +
            '</div>'
        );
        $('body').append(overlay);
    },

    /**
     * 隐藏全局加载遮罩
     */
    hideLoading: function() {
        $('#global-loading').remove();
    },

    /**
     * 自定义确认弹窗
     * @param {Object|String} options 配置对象或消息字符串
     * @param {Function} callback 回调函数，接收参数为true(确定)或false(取消)
     */
    confirm: function(options, callback) {
        var defaultOptions = {
            title: '确认操作',
            message: '您确定要执行此操作吗？',
            type: 'info', // 'info' 或 'danger'
            confirmText: '确定',
            cancelText: '取消'
        };
        
        if (typeof options === 'string') {
            options = { message: options };
        }
        
        var settings = $.extend(defaultOptions, options);
        var modalEl = document.getElementById('bcConfirmModal');
        var $modal = $(modalEl);
        
        // 设置内容
        $modal.find('#bcModalTitle').text(settings.title);
        $modal.find('#bcModalMessage').text(settings.message);
        $modal.find('#bcModalConfirm').text(settings.confirmText);
        $modal.find('#bcModalCancel').text(settings.cancelText).show();
        
        // 设置图标与样式
        var $iconWrap = $modal.find('#bcModalIcon');
        var $icon = $iconWrap.find('i');
        $icon.removeClass();
        
        if (settings.type === 'danger') {
            $iconWrap.css({ 'background': '#fee2e2', 'color': '#ef4444' });
            $icon.addClass('bi bi-exclamation-triangle');
        } else if (settings.type === 'warning') {
            $iconWrap.css({ 'background': '#fffbeb', 'color': '#f59e0b' });
            $icon.addClass('bi bi-exclamation-circle');
        } else {
            $iconWrap.css({ 'background': '#eff6ff', 'color': '#3b82f6' });
            $icon.addClass('bi bi-question-circle');
        }
        
        // 使用 Bootstrap 5 实例
        var modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
        modalInstance.show();
        
        // 绑定事件
        $modal.find('#bcModalConfirm').off('click').on('click', function() {
            modalInstance.hide();
            if (callback) callback(true);
        });
        
        $modal.find('#bcModalCancel').off('click').on('click', function() {
            modalInstance.hide();
            if (callback) callback(false);
        });
    },

    /**
     * 统一消息弹窗 (Alert模式)
     * @param {Object|String} options 配置对象或消息字符串
     * @param {Function} callback 回调函数
     */
    alert: function(options, callback) {
        var defaultOptions = {
            title: '系统提示',
            message: '操作成功',
            type: 'success', // 'success', 'warning', 'error', 'info'
            confirmText: '我知道了'
        };
        
        if (typeof options === 'string') {
            options = { message: options };
        }
        
        var settings = $.extend(defaultOptions, options);
        var modalEl = document.getElementById('bcConfirmModal');
        var $modal = $(modalEl);
        
        // 设置内容
        $modal.find('#bcModalTitle').text(settings.title);
        $modal.find('#bcModalMessage').text(settings.message);
        $modal.find('#bcModalConfirm').text(settings.confirmText);
        $modal.find('#bcModalCancel').hide();
        
        // 设置图标
        var $iconWrap = $modal.find('#bcModalIcon');
        var $icon = $iconWrap.find('i');
        $icon.removeClass();
        
        if (settings.type === 'success') {
            $iconWrap.css({ 'background': '#f0fdf4', 'color': '#16a34a' });
            $icon.addClass('bi bi-check-circle');
        } else if (settings.type === 'error') {
            $iconWrap.css({ 'background': '#fef2f2', 'color': '#dc2626' });
            $icon.addClass('bi bi-x-circle');
        } else if (settings.type === 'warning') {
            $iconWrap.css({ 'background': '#fffbeb', 'color': '#f59e0b' });
            $icon.addClass('bi bi-exclamation-circle');
        } else {
            $iconWrap.css({ 'background': '#eff6ff', 'color': '#3b82f6' });
            $icon.addClass('bi bi-info-circle');
        }
        
        var modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
        modalInstance.show();
        
        $modal.find('#bcModalConfirm').off('click').on('click', function() {
            modalInstance.hide();
            if (callback) callback(true);
        });
    },

    /**
     * 渲染分页插件
     * @param {Object} options 分页配置
     * @param {String} options.elem 分页容器选择器
     * @param {Number} options.total 总记录数
     * @param {Number} options.pages 总页数
     * @param {Number} options.current 当前页码
     * @param {Number} options.limit 每页条数
     * @param {Function} options.callback 翻页回调函数
     */
    renderPagination: function(options) {
        var settings = $.extend({
            elem: '#paginationList', // Default changed to #paginationList for unification
            container: null, 
            total: 0,
            pages: 0,
            current: 1,
            limit: 10,
            callback: null
        }, options);

        // Update total records if totalRecords element exists
        if ($('#totalRecords').length > 0) {
            $('#totalRecords').text(settings.total);
        }

        var target = settings.container || settings.elem;
        var container = $(target);
        
        // If container is not found and we are using default #paginationList, 
        // fallback to #pagination (for older pages)
        if (container.length === 0 && target === '#paginationList') {
            container = $('#pagination');
        }
        
        container.empty();

        if (settings.pages < 1) {
            return;
        }

        var pagination = container;

        // Previous page
        var prevClass = settings.current === 1 ? 'disabled' : '';
        var prevLink = $('<li class="page-item ' + prevClass + '"><a class="page-link" href="javascript:void(0)"><i class="bi bi-chevron-left"></i></a></li>');
        if (settings.current > 1) {
            prevLink.find('a').on('click', function() {
                if (settings.callback) settings.callback(settings.current - 1);
            });
        }
        pagination.append(prevLink);

        // Page numbers rendering logic (sliding window)
        var startPage = Math.max(1, settings.current - 2);
        var endPage = Math.min(settings.pages, startPage + 4);
        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        for (var i = startPage; i <= endPage; i++) {
            (function(pageNum) {
                var activeClass = pageNum === settings.current ? 'active' : '';
                var pageLink = $('<li class="page-item ' + activeClass + '"><a class="page-link" href="javascript:void(0)">' + pageNum + '</a></li>');
                if (pageNum !== settings.current) {
                    pageLink.find('a').on('click', function() {
                        if (settings.callback) settings.callback(pageNum);
                    });
                }
                pagination.append(pageLink);
            })(Math.max(1, i));
        }

        // Next page
        var nextClass = settings.current === settings.pages ? 'disabled' : '';
        var nextLink = $('<li class="page-item ' + nextClass + '"><a class="page-link" href="javascript:void(0)"><i class="bi bi-chevron-right"></i></a></li>');
        if (settings.current < settings.pages) {
            nextLink.find('a').on('click', function() {
                if (settings.callback) settings.callback(settings.current + 1);
            });
        }
        pagination.append(nextLink);
    },

    /**
     * 生成标准状态标签 HTML
     * @param {String} type 类型 (success, danger, warning, info, secondary)
     * @param {String} text 显示文本
     */
    getTag: function(type, text) {
        return '<span class="bc-tag bc-tag-' + (type || 'secondary') + '">' + this.escapeHtml(text || '-') + '</span>';
    },

    /**
     * 生成标准表格操作组 HTML
     * @param {Array} actions 操作配置数组 [{icon, title, onclick, class, type}]
     */
    getTableActions: function(actions) {
        if (!actions || !actions.length) return '';
        var html = '<div class="bc-table-actions">';
        actions.forEach(function(action) {
            var btnClass = 'bc-btn-icon';
            
            // 语义化类型映射
            var type = action.type;
            if (!type) {
                // 根据图标或标题猜测类型
                var icon = action.icon || '';
                var title = action.title || '';
                // 危险/退出：红色
                if (icon.indexOf('trash') > -1 || icon.indexOf('x-circle') > -1 || title.indexOf('删除') > -1 || title.indexOf('注销') > -1 || title.indexOf('移除') > -1 || title.indexOf('禁用') > -1) {
                    type = 'danger';
                } 
                // 添加/确认：绿色
                else if (icon.indexOf('plus') > -1 || icon.indexOf('check') > -1 || title.indexOf('添加') > -1 || title.indexOf('确认') > -1 || title.indexOf('通过') > -1 || title.indexOf('新增') > -1) {
                    type = 'success';
                } 
                // 编辑/查看/列表：蓝色
                else if (icon.indexOf('pencil') > -1 || icon.indexOf('eye') > -1 || icon.indexOf('search') > -1 || icon.indexOf('list') > -1 || title.indexOf('编辑') > -1 || title.indexOf('查看') > -1 || title.indexOf('修改') > -1 || title.indexOf('明细') > -1) {
                    type = 'primary';
                } 
                // 警示/权限/密钥/重置/安全：橙色
                else if (icon.indexOf('shield') > -1 || icon.indexOf('key') > -1 || icon.indexOf('lock') > -1 || icon.indexOf('arrow-counterclockwise') > -1 || title.indexOf('权限') > -1 || title.indexOf('重置') > -1 || title.indexOf('分配') > -1 || title.indexOf('策略') > -1 || title.indexOf('密码') > -1) {
                    type = 'warning';
                }
                // 明细/下载/导入/导出：成功绿色 (也可以是 Primary，根据风格选 Success 突出资产操作)
                else if (icon.indexOf('download') > -1 || icon.indexOf('upload') > -1 || title.indexOf('下载') > -1 || title.indexOf('导出') > -1 || title.indexOf('明细') > -1 || title.indexOf('记录') > -1) {
                    type = 'success';
                }
                // 审核/审核通过/驳回逻辑... 可继续扩展
            }
            
            if (type) btnClass += ' bc-btn-icon-' + type;
            if (action.class) btnClass += ' ' + action.class;
            
            var safeOnclick = (action.onclick || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            var safeTitle = (action.title || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            
            html += '<button class="' + btnClass + '" onclick="' + safeOnclick + '" title="' + safeTitle + '">' +
                    '<i class="bi ' + (action.icon || '') + '"></i>' +
                    '</button>';
        });
        html += '</div>';
        return html;
    },

    /**
     * 生成标准媒体资产 HTML (Logo/Avatar)
     * @param {String} url 图片地址
     * @param {String} type 类型 (logo, avatar)
     */
    getMedia: function(url, type) {
        var className = type === 'logo' ? 'bc-media-logo' : 'bc-media-avatar';
        if (!url) {
            // 兜底图或占位
            return '<div class="' + className + ' d-flex align-items-center justify-content-center bg-light text-muted">' +
                   '<i class="bi ' + (type === 'logo' ? 'bi-image' : 'bi-person') + '"></i>' +
                   '</div>';
        }
        return '<img src="' + this.escapeHtml(url) + '" class="' + className + '" alt="media">';
    },

    /**
     * 生成头像HTML (基于文字的兜底头像)
     * @param {String} name 用户名或昵称
     * @param {String} size 大小 (可选)
     */
    getAvatar: function(name, size) {
        if (!name) name = 'User';
        var firstChar = name.charAt(0).toUpperCase();
        var colors = [
            'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            'linear-gradient(135deg, #2af598 0%, #009efd 100%)',
            'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
            'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
            'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
            'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
        ];
        
        // 基于名称哈希选择颜色
        var hash = 0;
        for (var i = 0; i < name.length; i++) {
            hash = name.charCodeAt(i) + ((hash << 5) - hash);
        }
        var color = colors[Math.abs(hash) % colors.length];
        
        var style = 'background: ' + color + ';';
        if (size) {
            style += 'width: ' + size + '; height: ' + size + ';';
        }
        
        return '<div class="bc-avatar" style="' + style + '">' + this.escapeHtml(firstChar) + '</div>';
    },

    /**
     * 生成状态胶囊HTML
     * @param {Number} status 状态值 (0/1)
     * @param {String} text 文本 (启用/禁用)
     */
    getPill: function(status, text) {
        var type = status === 1 ? 'success' : 'secondary';
        return '<span class="bc-pill bc-pill-' + type + '">' + text + '</span>';
    },

    /**
     * 生成标签列表HTML
     * @param {String} tagsStr 逗号分隔的字符串
     */
    getTags: function(tagsStr) {
        if (!tagsStr) return '';
        var tags = tagsStr.split(',');
        var html = '';
        var that = this;
        tags.forEach(function(tag) {
            if (tag.trim()) {
                html += '<span class="bc-tag">' + that.escapeHtml(tag.trim()) + '</span>';
            }
        });
        return html;
    },

    /**
     * 现代浮动通知 (Toast)
     * @param {String} message 消息内容
     * @param {String} type 类型 (success, error, warning, info)
     * @param {Number} duration 持续时间 (ms, 默认3000)
     */
    toast: function(message, type, duration) {
        type = type || 'success';
        duration = duration || 3000;

        var container = $('#bc-toast-container');
        if (container.length === 0) {
            container = $('<div id="bc-toast-container" class="bc-toast-container"></div>');
            $('body').append(container);
        }

        var iconMap = {
            'success': 'bi-check-circle-fill',
            'error': 'bi-x-circle-fill',
            'warning': 'bi-exclamation-triangle-fill',
            'info': 'bi-info-circle-fill'
        };

        var toast = $('<div class="bc-toast bc-toast-' + type + '">' +
            '<i class="bi ' + iconMap[type] + '"></i>' +
            '<span class="bc-toast-message">' + this.escapeHtml(message) + '</span>' +
            '</div>');

        container.append(toast);

        // 触发进场动画
        setTimeout(function() { toast.addClass('show'); }, 10);

        // 自动移除
        setTimeout(function() {
            toast.removeClass('show');
            setTimeout(function() { toast.remove(); }, 500);
        }, duration);
    },

    /**
     * 锁定页面交互
     */
    lockPageInteractions: function() {
        // 只禁用内容区域的按钮（不包括侧边栏菜单、退出登录等）
        $('.main-content button, .content-wrapper button').prop('disabled', true);
        // 禁用内容区域的输入框
        $('.main-content input, .main-content select, .main-content textarea').prop('disabled', true);
        // 添加锁定标记
        $('body').addClass('page-locked');
    },

    /**
     * 解锁页面交互
     */
    unlockPageInteractions: function() {
        // 启用内容区域的按钮
        $('.main-content button, .content-wrapper button').prop('disabled', false);
        // 启用内容区域的输入框
        $('.main-content input, .main-content select, .main-content textarea').prop('disabled', false);
        // 移除锁定标记
        $('body').removeClass('page-locked');
    },

    /**
     * 显示加载遮罩（带计时器，用于同步任务）
     */
    showLoadingOverlay: function(name) {
        // 先移除已有遮罩
        $('#global-loading').remove();

        var overlay = $(
            '<div id="global-loading" class="bc-loading-overlay">' +
            '  <div class="bc-loading-box">' +
            '    <div class="spinner-border" role="status"></div>' +
            '    <p class="loading-text">' + this.escapeHtml(name) + '执行中，请稍候...</p>' +
            '    <p class="loading-timer" style="margin-top: 8px; font-size: 12px; color: #94a3b8;">已运行: 0秒</p>' +
            '  </div>' +
            '</div>'
        );
        $('body').append(overlay);
    },

    /**
     * 隐藏加载遮罩
     */
    hideLoadingOverlay: function() {
        $('#global-loading').remove();
    },

    /**
     * 更新计时器显示
     */
    updateLoadingTimer: function(seconds) {
        var timerEl = $('#global-loading .loading-timer');
        if (timerEl.length > 0) {
            timerEl.text('已运行: ' + seconds + '秒');
        }
    },

    /**
     * 触发全局同步（带锁定和轮询）
     */
    triggerGlobalSyncWithLock: function(url, name, refreshCallback) {
        var that = this;
        this.confirm({
            title: '确认执行',
            message: '确定要执行"' + name + '"吗？任务将在后台执行，同步期间您无法进行其他操作。',
            type: 'primary'
        }, function(ok) {
            if (ok) {
                // 1. 显示遮罩
                that.showLoadingOverlay(name);

                // 2. 锁定所有交互
                that.lockPageInteractions();

                // 3. 发送同步请求
                $.ajax({
                    url: url,
                    type: 'POST',
                    success: function(res) {
                        if (res.code === 200) {
                            that.toast(res.message || name + '已触发', 'success');
                            // 4. 开始轮询检查状态
                            that.pollSyncStatus(name, refreshCallback);
                        } else {
                            that.toast(res.message || '触发失败', 'error');
                            that.unlockPageInteractions();
                            that.hideLoadingOverlay();
                        }
                    },
                    error: function() {
                        that.toast(name + '触发失败', 'error');
                        that.unlockPageInteractions();
                        that.hideLoadingOverlay();
                    }
                });
            }
        });
    },

    /**
     * 轮询检查同步状态
     */
    pollSyncStatus: function(name, refreshCallback) {
        var that = this;
        var pollCount = 0;
        var maxPolls = 900; // 最多轮询75分钟（5秒一次）
        var syncStartTime = Date.now();

        function check() {
            $.ajax({
                url: '/api/ihr/sync-status',
                type: 'GET',
                success: function(res) {
                    if (res.code === 200 && res.data) {
                        var syncing = res.data.syncing;

                        // 更新已运行时间（优先用后端返回的，如果没有则自己计算）
                        if (res.data.elapsedSeconds !== undefined) {
                            that.updateLoadingTimer(res.data.elapsedSeconds);
                        } else {
                            var elapsed = Math.floor((Date.now() - syncStartTime) / 1000);
                            that.updateLoadingTimer(elapsed);
                        }

                        if (syncing) {
                            // 还在同步中
                            pollCount++;
                            if (pollCount < maxPolls) {
                                setTimeout(check, 5000); // 5秒后再次检查
                            } else {
                                // 超时处理
                                that.handleSyncTimeout(name, refreshCallback);
                            }
                        } else {
                            // 同步完成
                            that.syncCompleted(name, refreshCallback);
                        }
                    } else {
                        // 接口返回异常，继续轮询
                        pollCount++;
                        if (pollCount < maxPolls) {
                            setTimeout(check, 5000);
                        } else {
                            that.handleSyncTimeout(name, refreshCallback);
                        }
                    }
                },
                error: function() {
                    // 请求失败，继续轮询
                    pollCount++;
                    if (pollCount < maxPolls) {
                        setTimeout(check, 5000);
                    } else {
                        that.handleSyncTimeout(name, refreshCallback);
                    }
                }
            });
        }

        check();
    },

    /**
     * 处理同步超时
     */
    handleSyncTimeout: function(name, refreshCallback) {
        this.toast(name + '执行时间过长，请稍后刷新页面查看结果', 'warning');
        this.unlockPageInteractions();
        this.hideLoadingOverlay();
        if (refreshCallback && typeof refreshCallback === 'function') {
            refreshCallback();
        }
    },

    /**
     * 同步完成处理
     */
    syncCompleted: function(name, refreshCallback) {
        this.toast(name + '执行完成', 'success');
        this.unlockPageInteractions();
        this.hideLoadingOverlay();
        if (refreshCallback && typeof refreshCallback === 'function') {
            refreshCallback();
        }
    },

    /**
     * 页签栏管理模块
     */
    TabBar: {
        STORAGE_KEY: 'bcTabBarTabs',

        getTabs: function() {
            try {
                var data = sessionStorage.getItem(this.STORAGE_KEY);
                return data ? JSON.parse(data) : [];
            } catch (e) {
                return [];
            }
        },

        saveTabs: function(tabs) {
            sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(tabs));
        },

        addTab: function(path, title, icon, reorder) {
            if (!path || path === '#') return;
            var tabs = this.getTabs();
            var existing = tabs.find(function(t) { return t.path === path; });
            if (existing) {
                // 已存在：只更新标题/图标，不改变位置（除非明确要求重排）
                existing.title = title || existing.title;
                existing.icon = icon || existing.icon;
                if (reorder) {
                    tabs = tabs.filter(function(t) { return t.path !== path; });
                    tabs.push(existing);
                }
            } else {
                tabs.push({ path: path, title: title || path, icon: icon || '' });
            }
            if (tabs.length > 20) {
                tabs.shift();
            }
            this.saveTabs(tabs);
        },

        removeTab: function(path) {
            var tabs = this.getTabs();
            var removed = null;
            var wasActive = false;
            var currentPath = window.location.pathname;

            tabs = tabs.filter(function(t) {
                if (t.path === path) {
                    removed = t;
                    wasActive = (path === currentPath);
                    return false;
                }
                return true;
            });

            this.saveTabs(tabs);
            return { removed: removed, tabs: tabs, wasActive: wasActive };
        },

        ensureDOM: function() {
            if (!$('#tabBar').length) {
                $('.top-navbar').after(
                    '<div class="tab-bar" id="tabBar">' +
                    '  <div class="tab-bar-scroll" id="tabBarScroll">' +
                    '    <div class="tab-bar-items" id="tabBarItems"></div>' +
                    '  </div>' +
                    '</div>'
                );
            }
        },

        renderTabs: function() {
            this.ensureDOM();
            var container = $('#tabBarItems');
            if (!container.length) return;

            var tabs = this.getTabs();
            var currentPath = window.location.pathname;
            container.empty();

            if (tabs.length === 0) {
                $('#tabBar').removeClass('tab-bar-visible');
                $('.content-wrapper').css('margin-top', 'var(--header-height)');
                return;
            }

            $('#tabBar').addClass('tab-bar-visible');
            $('.content-wrapper').css('margin-top', 'calc(var(--header-height) + var(--tab-bar-height))');

            var that = this;
            tabs.forEach(function(tab) {
                var isActive = (tab.path === currentPath) ||
                    (currentPath === '/' && tab.path === '/') ||
                    (currentPath === '/index' && tab.path === '/') ||
                    (tab.path !== '/' && currentPath.startsWith(tab.path + '/'));
                var isDashboard = (tab.path === '/');

                var item = $(
                    '<div class="tab-bar-item' + (isActive ? ' active' : '') + (isDashboard ? ' tab-bar-pinned' : '') + '" data-path="' +
                    BC.escapeHtml(tab.path) + '">' +
                    (tab.icon ? '<i class="bi ' + BC.escapeHtml(tab.icon) + ' tab-icon"></i>' : '') +
                    '<span class="tab-title">' + BC.escapeHtml(tab.title) + '</span>' +
                    (isDashboard ? '' : '<span class="tab-bar-close" title="关闭"><i class="bi bi-x"></i></span>') +
                    '</div>'
                );

                item.on('click', function(e) {
                    if ($(e.target).closest('.tab-bar-close').length) return;
                    var targetPath = $(this).attr('data-path');
                    if (targetPath && targetPath !== currentPath) {
                        window.location.href = targetPath;
                    }
                });

                item.find('.tab-bar-close').on('click', function(e) {
                    e.stopPropagation();
                    var targetPath = $(this).closest('.tab-bar-item').attr('data-path');
                    that.handleClose(targetPath);
                });

                container.append(item);
            });

            var activeTab = container.find('.tab-bar-item.active');
            if (activeTab.length) {
                var scrollContainer = $('#tabBarScroll')[0];
                var tabEl = activeTab[0];
                scrollContainer.scrollLeft = tabEl.offsetLeft - scrollContainer.offsetLeft - 20;
            }
        },

        handleClose: function(path) {
            // 仪表盘页签不可关闭
            if (path === '/') return;

            var result = this.removeTab(path);

            if (result.wasActive) {
                var remaining = result.tabs;
                if (remaining.length > 0) {
                    window.location.href = remaining[remaining.length - 1].path;
                } else {
                    window.location.href = '/';
                }
            } else {
                this.renderTabs();
            }
        },

        init: function() {
            var currentPath = window.location.pathname;

            // 主页/仪表盘：始终作为第一个固定页签
            if (currentPath === '/' || currentPath === '/index') {
                this.addTab('/', '仪表盘', 'bi-speedometer2');
                this.renderTabs();
                return;
            }

            var title = '';
            var icon = '';

            var activeMenu = $('.menu-item.active');
            if (activeMenu.length > 0) {
                var fullText = activeMenu.contents().filter(function() {
                    return this.nodeType === 3;
                }).text().trim();
                title = fullText || document.title.replace(' - BC体育后台管理系统', '').replace(' - BC体育巅峰后台', '');

                var menuIcon = activeMenu.find('i.bi').first();
                if (menuIcon.length > 0) {
                    var classes = menuIcon.attr('class').split(' ');
                    var biClass = classes.find(function(c) {
                        return c.startsWith('bi-') && c !== 'bi-chevron-down';
                    });
                    if (biClass) icon = biClass;
                }
            } else {
                title = document.title.replace(' - BC体育后台管理系统', '').replace(' - BC体育巅峰后台', '');
            }

            this.addTab(currentPath, title, icon);
            // 确保仪表盘页签始终存在
            var tabs = this.getTabs();
            var hasDashboard = tabs.some(function(t) { return t.path === '/'; });
            if (!hasDashboard) {
                this.addTab('/', '仪表盘', 'bi-speedometer2');
            }
            this.renderTabs();
        }
    }
};

// 加载菜单树函数
function loadMenuTree() {
    // 检查是否有缓存的菜单数据
    var cachedMenus = sessionStorage.getItem('cachedMenus');
    if (cachedMenus) {
        try {
            var menuData = JSON.parse(cachedMenus);
            renderMenu(menuData);
            return;
        } catch (e) {
            console.error('解析缓存菜单数据失败', e);
        }
    }
    
    // 从服务器获取菜单数据
    $.ajax({
        url: '/api/menu/userTree',
        type: 'GET',
        success: function(data) {
            if (data.code === 200) {
                // 缓存菜单数据到sessionStorage
                sessionStorage.setItem('cachedMenus', JSON.stringify(data.data));
                renderMenu(data.data);
            }
        },
        error: function() {
            console.error('加载菜单失败');
            // 如果加载失败，显示默认菜单
            renderDefaultMenu();
        }
    });
}

// 渲染菜单函数
function renderMenu(menus) {
    var menuContainer = $('#menuContainer');
    menuContainer.empty();
    
    // 添加仪表盘菜单（默认）
    var dashboardItem = $('<a href="/" class="menu-item"><i class="bi bi-speedometer2"></i> 仪表盘</a>');
    menuContainer.append(dashboardItem);
    
    // 递归渲染菜单
    renderMenuItems(menus, menuContainer, 0);
    
    // 设置当前页面菜单项为激活状态并同步标题
    setTimeout(function() {
        setActiveMenu();
        updateNavbarTitle();
    }, 100);
}

// 递归渲染菜单项
function renderMenuItems(menus, container, level) {
    $.each(menus, function(index, menu) {
        // 核心修复：如果菜单标记为不可见（visible != 1），则跳过侧边栏渲染
        if (menu.visible !== 1) {
            return true; // continue
        }

        var menuItem = $('<a href="#" class="menu-item"></a>');
        
        // 动态计算缩进：基础24px，每级增加18px，适配未来多级菜单
        var indent = 24 + (level * 18);
        menuItem.css('padding-left', indent + 'px');
        
        // 添加自定义样式类（如果菜单配置了样式）
        if (menu.customClass) {
            menuItem.addClass(menu.customClass);
        }
        
        // 添加菜单标题属性（用于tooltip）
        if (menu.description) {
            menuItem.attr('title', menu.description);
        }
        
        // 核心修复：检查是否有【可见】的子项，用于判断是否渲染下拉箭头
        var visibleChildren = [];
        if (menu.children && menu.children.length > 0) {
            visibleChildren = menu.children.filter(c => c.visible === 1);
        }
        
        if (visibleChildren.length > 0) {
            // 有可见子菜单（目录）
            menuItem.attr('data-bs-toggle', 'collapse');
            menuItem.attr('data-bs-target', '#menu' + menu.id);

            // 动态生成图标和文本
            var iconClass = BC.escapeHtml(menu.icon || 'bi-folder');
            var menuText = BC.escapeHtml(menu.menuName);

            // 如果配置了颜色，添加到图标
            if (menu.iconColor) {
                iconClass += ' text-' + BC.escapeHtml(menu.iconColor);
            }

            menuItem.html('<i class="bi ' + iconClass + '"></i> ' + menuText + '<i class="bi bi-chevron-down ms-auto"></i>');

            var subMenu = $('<div class="collapse submenu" id="menu' + menu.id + '"></div>');
            renderMenuItems(visibleChildren, subMenu, level + 1);

            container.append(menuItem);
            container.append(subMenu);
        } else {
            // 无子菜单（菜单项）
            var href = menu.path || '#';
            menuItem.attr('href', href);

            // 动态生成图标和文本
            var iconClass = BC.escapeHtml(menu.icon || 'bi-file-earmark');
            var menuText = BC.escapeHtml(menu.menuName);

            // 如果配置了颜色，添加到图标
            if (menu.iconColor) {
                iconClass += ' text-' + BC.escapeHtml(menu.iconColor);
            }

            // 如果配置了徽章
            if (menu.badge) {
                menuText += ' <span class="badge bg-' + BC.escapeHtml(menu.badgeColor || 'primary') + ' ms-1">' + BC.escapeHtml(menu.badge) + '</span>';
            }

            menuItem.html('<i class="bi ' + iconClass + '"></i> ' + menuText);
            container.append(menuItem);
        }
    });
}

// 渲染默认或最小菜单（当动态加载失败时）
function renderDefaultMenu() {
    var menuContainer = $('#menuContainer');
    menuContainer.empty();
    
    // 默认最小菜单结构
    var defaultMenu = `
        <a href="/" class="menu-item active">
            <i class="bi bi-speedometer2"></i> 仪表盘
        </a>
        <a href="/help" class="menu-item">
            <i class="bi bi-question-circle"></i> 帮助中心
        </a>
    `;
    
    menuContainer.html(defaultMenu);
    setActiveMenu();
}

// 设置当前页面菜单项为激活状态
function setActiveMenu() {
    var currentPath = window.location.pathname;
    
    // 移除所有active类
    $('.menu-item').removeClass('active');
    
    // 根据当前路径设置激活状态
    if (currentPath === '/' || currentPath === '/index') {
        $('a[href="/"]').addClass('active');
    } else {
        // 找到最匹配的菜单项
        var bestMatch = null;
        var bestMatchLength = 0;
        
        $('.menu-item').each(function() {
            var href = $(this).attr('href');
            if (href && href !== '#' && href !== '/') {
                // 检查当前路径是否以菜单路径开头
                if (currentPath === href || currentPath.startsWith(href + '/') || currentPath.startsWith(href + '?')) {
                    // 选择最长匹配的路径
                    if (href.length > bestMatchLength) {
                        bestMatch = $(this);
                        bestMatchLength = href.length;
                    }
                }
            }
        });
        
        if (bestMatch) {
            bestMatch.addClass('active');
            
            // 如果是子菜单项，展开父菜单
            var parentCollapse = bestMatch.closest('.collapse');
            if (parentCollapse.length > 0) {
                parentCollapse.addClass('show');
            }
        } else {
            // 如果没有找到精确匹配，尝试模糊匹配
            $('.menu-item').each(function() {
                var href = $(this).attr('href');
                if (href && href !== '#' && href !== '/') {
                    // 检查菜单路径是否是当前路径的一部分
                    if (currentPath.indexOf(href) !== -1) {
                        $(this).addClass('active');
                        
                        // 如果是子菜单项，展开父菜单
                        var parentCollapse = $(this).closest('.collapse');
                        if (parentCollapse.length > 0) {
                            parentCollapse.addClass('show');
                        }
                        return false;
                    }
                }
            });
        }
    }
}

// 保存当前选中的菜单路径
function saveCurrentMenuPath(path) {
    sessionStorage.setItem('currentMenuPath', path);
}

// 恢复上次选中的菜单
function restoreMenuSelection() {
    var savedPath = sessionStorage.getItem('currentMenuPath');
    if (savedPath) {
        $('.menu-item').each(function() {
            var href = $(this).attr('href');
            if (href === savedPath) {
                $(this).addClass('active');
                var parentCollapse = $(this).closest('.collapse');
                if (parentCollapse.length > 0) {
                    parentCollapse.addClass('show');
                }
                return false;
            }
        });
    }
}

// 触发导航栏横向扩展效果
function triggerNavbarExpansion() {
    var navbar = $('.top-navbar');
    navbar.addClass('expanded');
    
    // 更新导航栏标题为当前页面名称
    updateNavbarTitle();
    
    // 3秒后移除扩展效果
    setTimeout(function() {
        navbar.removeClass('expanded');
    }, 3000);
}

// 更新导航栏标题 (根据当前激活菜单动态获取)
function updateNavbarTitle() {
    var titleContainer = $('#navbarTitle');
    var pageTitleEl = titleContainer.find('.page-title');
    var titleIconEl = titleContainer.find('.title-icon');
    
    // 默认兜底信息
    var pageTitle = '巅峰探索 · 后台中心';
    var iconClass = 'bi-compass'; 

    // 尝试从左侧菜单获取当前激活项
    var activeMenu = $('.menu-item.active');
    
    if (activeMenu.length > 0) {
        // 获取文本内容 (去掉小徽章等干扰)
        var fullText = activeMenu.contents().filter(function() {
            return this.nodeType === 3; // 只取文本节点
        }).text().trim();
        
        if (fullText) pageTitle = fullText;
        
        // 获取图标
        var menuIcon = activeMenu.find('i.bi').first();
        if (menuIcon.length > 0) {
            // 获取图标名，过滤掉通用样式
            var classes = menuIcon.attr('class').split(' ');
            var biClass = classes.find(c => c.startsWith('bi-') && c !== 'bi-chevron-down');
            if (biClass) iconClass = biClass;
        }
    } else {
        // 如果是仪表盘首页
        if (window.location.pathname === '/' || window.location.pathname === '/index') {
            pageTitle = '系统仪表盘';
            iconClass = 'bi-speedometer2';
        }
    }
    
    // 应用更新并触发动画效果
    // 先移除动画类以允许重新触发
    pageTitleEl.css('animation', 'none');
    titleIconEl.css('animation', 'none');
    
    setTimeout(function() {
        pageTitleEl.text(pageTitle).css('animation', '');
        
        if (iconClass) {
            titleIconEl.removeClass('d-none').attr('class', 'title-icon bi ' + iconClass).css('animation', '');
        } else {
            titleIconEl.addClass('d-none');
        }
    }, 10);
}

// 初始化菜单
$(document).ready(function() {
    // 加载动态菜单
    loadMenuTree();
    
    // 恢复菜单选择
    restoreMenuSelection();
    
    // 初始化导航栏标题
    updateNavbarTitle();

    // 初始化页签栏（延迟等待菜单渲染完成）
    setTimeout(function() {
        BC.TabBar.init();
    }, 150);

    // 侧边栏切换
    $('#sidebarToggle').click(function() {
        $('#sidebar').toggleClass('show');
    });
    
    // 菜单项点击（使用事件委托）
    $(document).on('click', '.menu-item', function(e) {
        if (!$(this).attr('data-bs-toggle')) {
            $('.menu-item').removeClass('active');
            $(this).addClass('active');

            // 触发导航栏交互与标题同步
            triggerNavbarExpansion();
            updateNavbarTitle();

            // 保存当前选中的菜单路径
            var href = $(this).attr('href');
            if (href && href !== '#') {
                saveCurrentMenuPath(href);

                // 添加页签到页签栏
                var menuText = $(this).contents().filter(function() {
                    return this.nodeType === 3;
                }).text().trim();
                var menuIconEl = $(this).find('i.bi').first();
                var iconClass = '';
                if (menuIconEl.length > 0) {
                    var cls = menuIconEl.attr('class').split(' ');
                    iconClass = cls.find(function(c) {
                        return c.startsWith('bi-') && c !== 'bi-chevron-down';
                    }) || '';
                }
                BC.TabBar.addTab(href, menuText, iconClass);
            }
        }
    });
    
    // 退出登录
    $('#logoutBtn').click(function(e) {
        e.preventDefault();
        BC.confirm({
            title: '确认退出',
            message: '您确定要退出当前账号并离开系统吗？',
            type: 'info',
            confirmText: '确认退出',
            cancelText: '我再想想'
        }, function(confirmed) {
            if (confirmed) {
                // 清除菜单缓存
                sessionStorage.removeItem('cachedMenus');
                sessionStorage.removeItem('currentMenuPath');
                sessionStorage.removeItem('bcTabBarTabs');
                
                $.ajax({
                    url: '/doLogout',
                    type: 'POST',
                    success: function(response) {
                        window.location.href = '/login';
                    },
                    error: function() {
                        alert('退出登录失败');
                    }
                });
            }
        });
    });
    
    // 响应式侧边栏
    $(window).resize(function() {
        if ($(window).width() > 768) {
            $('#sidebar').removeClass('show');
        }
    });

    // 被踢出跳转登录页（防止重复弹窗）
    var _kickedOut = false;
    function handleKickedOut(msg) {
        if (_kickedOut) return;
        _kickedOut = true;
        BC.alert({
            title: '登录提示',
            message: msg || '您的账号已在其他设备登录',
            type: 'warning',
            confirmText: '重新登录'
        }, function() {
            window.location.href = '/login?kicked=1';
        });
    }

    // 轮询检测账号被踢出（每10秒检查一次）
    var _sessionFailCount = 0;
    if (window.location.pathname !== '/login') {
        setInterval(function() {
            $.ajax({
                url: '/api/session/check',
                type: 'GET',
                success: function(res) {
                    _sessionFailCount = 0;
                    if (res.code !== 200) {
                        handleKickedOut(res.message);
                    }
                },
                error: function() {
                    _sessionFailCount++;
                    if (_sessionFailCount >= 3) {
                        handleKickedOut('网络连接异常，请重新登录');
                    }
                }
            });
        }, 10000);
    }

    // 全局AJAX会话失效检测（兜底）
    $(document).ajaxComplete(function(event, xhr, settings) {
        if (window.location.pathname === '/login') return;
        if (settings && settings.url && settings.url.indexOf('/api/session/check') >= 0) return;

        if (xhr.status === 200) {
            var ct = xhr.getResponseHeader('Content-Type');
            if (ct && ct.indexOf('text/html') >= 0 && xhr.responseText) {
                if (xhr.responseText.indexOf('id="loginForm"') >= 0) {
                    handleKickedOut('您的账号已在其他设备登录，请重新登录');
                }
            }
        }
    });
});