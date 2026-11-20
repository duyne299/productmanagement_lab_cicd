package com.vti.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.vti.entity.Account;
import com.vti.form.AccountFormForCreating;
import com.vti.repository.IAccountRepository;

class AccountServiceTest {

	/**
	 * AccountRepository giả: chỉ hỗ trợ save() và findByUsername().
	 */
	private static IAccountRepository fakeRepository() {
		return (IAccountRepository) Proxy.newProxyInstance(IAccountRepository.class.getClassLoader(),
				new Class<?>[] { IAccountRepository.class }, (proxy, method, args) -> {
					if ("save".equals(method.getName())) {
						return args[0];
					}
					if ("findByUsername".equals(method.getName())) {
						return null;
					}
					return null;
				});
	}

	private static AccountFormForCreating newForm(String rawPassword) {
		AccountFormForCreating form = new AccountFormForCreating();
		form.setEmail("test@vti.com");
		form.setUsername("testuser");
		form.setFullname("Test User");
		form.setMobile("0900000000");
		form.setAddress("Ha Noi");
		form.setPassword(rawPassword);
		return form;
	}

	@Test
	void createNewAccount_ShouldStoreBcryptHash_NotPlainPassword() {
		AccountService service = new AccountService();
		ReflectionTestUtils.setField(service, "accountRepository", fakeRepository());
		ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder());

		String rawPassword = "123456";

		Account created = service.createNewAccount(newForm(rawPassword));

		assertNotEquals(rawPassword, created.getPassword(), "Mật khẩu phải được băm trước khi lưu");
		assertTrue(created.getPassword().startsWith("$2"),
				"Mật khẩu phải là BCrypt hash để khớp với BCryptPasswordEncoder của Spring Security");
		assertTrue(new BCryptPasswordEncoder().matches(rawPassword, created.getPassword()),
				"BCryptPasswordEncoder phải xác thực được mật khẩu gốc");
	}

	@Test
	void createNewAccount_ShouldKeepRawPasswordRecoverableOnlyThroughBcrypt() {
		AccountService service = new AccountService();
		ReflectionTestUtils.setField(service, "accountRepository", fakeRepository());
		ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder());

		String rawPassword = "P@ssw0rd!";
		Account created = service.createNewAccount(newForm(rawPassword));

		assertEquals(true, new BCryptPasswordEncoder().matches(rawPassword, created.getPassword()));
		assertEquals(false, new BCryptPasswordEncoder().matches("sai-mat-khau", created.getPassword()));
	}
}
