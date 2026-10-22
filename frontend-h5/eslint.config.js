import js from '@eslint/js'
import globals from 'globals'
import pluginVue from 'eslint-plugin-vue'
import prettierConfig from 'eslint-config-prettier'

// 纯 JS 工程（不引 TypeScript），规则对齐参考工程 mall-client
export default [
  {
    ignores: ['dist/**', 'node_modules/**', 'coverage/**']
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
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'vue/attributes-order': 'off',
      'vue/no-v-html': 'off',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'no-console': 'off'
    }
  },
  prettierConfig
]
