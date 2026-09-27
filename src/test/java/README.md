# Running JUnit Tests in IntelliJ

When running JUnit tests in IntelliJ, you may see an error such as:

**InaccessibleObjectException: Unable to make … accessible:**

**module … does not "opens …" to module
org.junit.platform.commons**

This happens because newer IntelliJ does not automatically place JUnit on the module path for modular projects.

Follow these steps:

1. **Open `module-info.java`.**
2. **Find the following line and uncomment it:**

   **// Uncomment the following line when running tests in IntelliJ:**

   **// requires org.junit.jupiter.api;**

3. **IntelliJ will now highlight the `requires org.junit.jupiter.api;` line in red.**
4. **Place your cursor on the red underline. IntelliJ will show a quick-fix option:**

   **Add library `org.junit.jupiter.api` to classpath**

5. **Click this option. IntelliJ will configure the test module path automatically.**

After this, JUnit tests will run normally.

> **Note:** This IntelliJ configuration is local and not portable.

# Test Warning (Mockito)

**When running tests using Mockito, you may see a Java agent warning.**

- This is caused by internal mechanisms used for mocking.
- It does not affect test results.
- No action is required 😄.