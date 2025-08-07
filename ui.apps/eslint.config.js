import globals from 'globals';
import standard from 'neostandard';

export default [
    ...standard(),
    {
        files: ['**/*.js'],
        languageOptions: {
            ecmaVersion: 2020,
            sourceType: 'module',
            parserOptions: {
                projectService: true
            },
            globals: {
                // Default global variables
                ...globals.browser,
                ...globals.jquery,
                // Custom global variables
                Granite: 'writable',
                Coral: 'readonly'
            }
        },
        linterOptions: {
            reportUnusedDisableDirectives: 'warn'
        }
    },
    {
        rules: {
            'no-new-func': 'off',
            'no-useless-call': 'off',
            '@stylistic/indent': [
                'error', 4, {
                    SwitchCase: 1
                }
            ],
            '@stylistic/operator-linebreak': [2, 'after'],
            '@stylistic/semi': [1, 'always'],
            '@stylistic/space-before-function-paren': [
                'error',
                {
                    anonymous: 'always',
                    named: 'never',
                    asyncArrow: 'never'
                }
            ]
        }
    }
];
