// 登录和注册功能的JavaScript代码
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const loginTab = document.getElementById('login-tab');
    const registerTab = document.getElementById('register-tab');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');

    // 切换表单显示
    loginTab.addEventListener('click', function() {
        loginTab.classList.add('active');
        registerTab.classList.remove('active');
        loginForm.classList.add('active-form');
        loginForm.classList.remove('hidden-form');
        registerForm.classList.add('hidden-form');
        registerForm.classList.remove('active-form');
    });

    registerTab.addEventListener('click', function() {
        registerTab.classList.add('active');
        loginTab.classList.remove('active');
        registerForm.classList.add('active-form');
        registerForm.classList.remove('hidden-form');
        loginForm.classList.add('hidden-form');
        loginForm.classList.remove('active-form');
    });

    // 登录表单提交
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        
        // 验证表单
        if (!email || !password) {
            alert('请填写所有必填字段');
            return;
        }
        
        // 准备登录数据
        const loginData = {
            email: email,
            password: password
        };
        
        // 发送登录请求
        fetch('/userInfo/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                // 登录成功，保存token并跳转到聊天页面
                localStorage.setItem('token', data.data.token);
                localStorage.setItem('userId', data.data.userId);
                localStorage.setItem('nickName', data.data.nickName);
                window.location.href = '/chat.html';
            } else {
                // 登录失败，显示错误信息
                alert('登录失败: ' + data.msg);
            }
        })
        .catch(error => {
            console.error('登录请求出错:', error);
            alert('登录请求出错，请稍后再试');
        });
    });

    // 注册表单提交
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const email = document.getElementById('register-email').value;
        const nickname = document.getElementById('register-nickname').value;
        const password = document.getElementById('register-password').value;
        const confirmPassword = document.getElementById('register-confirm-password').value;
        const sex = document.querySelector('input[name="sex"]:checked').value;
        
        // 验证表单
        if (!email || !nickname || !password || !confirmPassword) {
            alert('请填写所有必填字段');
            return;
        }
        
        if (password !== confirmPassword) {
            alert('两次输入的密码不一致');
            return;
        }
        
        // 准备注册数据
        const registerData = {
            email: email,
            nickName: nickname,
            password: password,
            sex: sex === '1'
        };
        
        // 发送注册请求
        fetch('/userInfo/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(registerData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                // 注册成功，显示成功消息并切换到登录表单
                alert('注册成功，请登录');
                loginTab.click();
            } else {
                // 注册失败，显示错误信息
                alert('注册失败: ' + data.msg);
            }
        })
        .catch(error => {
            console.error('注册请求出错:', error);
            alert('注册请求出错，请稍后再试');
        });
    });
});