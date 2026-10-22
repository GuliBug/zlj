import js from '@eslint/js'
import globals from 'globals'
import pluginVue from 'eslint-plugin-vue'
import prettierConfig from 'eslint-config-prettier'

export default [
  {
    ignores: ['dist/**', 'node_modules/**', 'coverage/**', 'tests/e2e/screenshots/**']
  },
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      // 页面组件以业务名命名，无需强制多单词
      'vue/multi-word-component-names': 'off',
      // 本项目 props 均为可选，且已有默认值或允许为空
      'vue/require-default-prop': 'off',
      // 属性顺序交给 prettier 之外的团队习惯，不做强制
      'vue/attributes-order': 'off',
      'vue/no-v-html': 'off',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      // 调试与降级日志是需要的
      'no-console': 'off'
    }
  },
  prettierConfig
]
