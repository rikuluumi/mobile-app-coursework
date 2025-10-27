<?php
/**
 * Plugin Name: Mobile app course backend
 * Description: Adds a REST API endpoint for mobile app.
 * Version: 1.0
 * Author: Riku Luumi
 */

add_action('rest_post_dispatch', function ($result) {
    header('Cache-Control: no-cache, must-revalidate, max-age=0');
    return $result;
});

add_action('rest_api_init', function () {
	register_rest_route('visitorlog/v1', '/login', [
        'methods' => 'POST',
        'callback' => 'visitor_log_login',
        'permission_callback' => '__return_true'
    ]);
	
	register_rest_route('recipes/v1', '/popular', [
        'methods' => 'GET',
        'callback' => 'get_popular_recipes',
        'permission_callback' => 'authenticate_callback',
    ]);
	
	register_rest_route('recipes/v1', '/add', [
        'methods'  => 'POST',
        'callback' => 'add_new_recipe',
        'permission_callback' => 'authenticate_callback',
    ]);
	
	register_rest_route('recipes/v1', '/my', [
		'methods' => 'GET',
		'callback' => 'get_user_recipes',
		'permission_callback' => 'authenticate_callback',
	]);
	
	register_rest_route('recipes/v1', '/recipe', [
		'methods' => 'GET',
		'callback' => 'get_recipe',
		'permission_callback' => 'authenticate_callback',
	]);
	
	register_rest_route('recipes/v1', '/favorite', [
		'methods' => ['POST', 'DELETE'],
		'callback' => 'toggle_favorite',
		'permission_callback' => 'authenticate_callback',
	]);
	
	register_rest_route('recipes/v1', '/search', [
		'methods' => ['POST'],
		'callback' => 'search_recipes',
		'permission_callback' => 'authenticate_callback',
	]);
});

function authenticate_callback(WP_REST_Request $request) {
	$auth_header = $request->get_header('Authorization');
    if (empty($auth_header)) {
        error_log('AUTH FAIL: Missing header');
        return false;
    }

    $token = trim(str_replace('Bearer', '', $auth_header));

    global $wpdb;
    $user_query = new WP_User_Query([
        'meta_key' => '_visitor_log_token',
        'meta_value' => $token,
        'number' => 1,
    ]);

    $users = $user_query->get_results();
    if (empty($users)) {
        error_log('AUTH FAIL: No matching token found (' . $token . ')');
        return false;
    }

    $user = $users[0];
    wp_set_current_user($user->ID);
    wp_set_auth_cookie($user->ID);

    return true;
}

function visitor_log_login(WP_REST_Request $request) {
	$username = sanitize_text_field($request->get_param('username'));
    $password = $request->get_param('password');

    if (empty($username) || empty($password)) {
        return new WP_Error('missing_credentials', 'Username or password missing', ['status' => 400]);
    }

    $user = wp_authenticate($username, $password);

    if (is_wp_error($user)) {
        return new WP_Error('invalid_credentials', 'Invalid username or password', ['status' => 401]);
    }

    $token = wp_generate_password(32, false);
    update_user_meta($user->ID, '_visitor_log_token', $token);

    return [
        'success' => true,
        'user_id' => $user->ID,
        'username' => $user->user_login,
        'token' => $token,
    ];
}

function get_popular_recipes(WP_REST_Request $request) {
	global $wpdb;
	
	$user = wp_get_current_user();
	
	if (!$user) {
        return new WP_Error('unauthorized', 'Invalid or expired token', ['status' => 401]);
    }
	
	$table_name = $wpdb->prefix . 'recipes';
	
	$results = $wpdb->get_results("
        SELECT * FROM $table_name
        ORDER BY favorites_count DESC
        LIMIT 10
    ");

    return ['success' => true, 'data' => $results];
}

function add_new_recipe(WP_REST_Request $request) {
    global $wpdb;
	
	$user = wp_get_current_user();
	
	if (!$user) {
        return new WP_Error('unauthorized', 'Invalid or expired token', ['status' => 401]);
    }
	
    $user_id = $user->ID;
    $table_name = $wpdb->prefix . 'recipes';

    $title = sanitize_text_field($request->get_param('title'));
    $description = sanitize_textarea_field($request->get_param('description'));
    $image_url = esc_url_raw($request->get_param('image_url'));

    if (empty($title) || empty($description)) {
        return new WP_Error('missing_fields', 'Title and description are required.', ['status' => 400]);
    }

    if (!$user_id) {
        return new WP_Error('unauthorized', 'User not logged in.', ['status' => 401]);
    }

    $wpdb->query("CREATE TABLE IF NOT EXISTS $table_name (
        id BIGINT(20) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT(20) UNSIGNED NOT NULL,
        title VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        image_url TEXT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) {$wpdb->get_charset_collate()};");

    $inserted = $wpdb->insert($table_name, [
        'user_id'     => $user_id,
        'title'       => $title,
        'description' => $description,
        'image_url'   => $image_url,
    ]);

    if ($inserted === false) {
        return new WP_Error('db_insert_failed', 'Failed to save recipe.', ['status' => 500, 'debug' => $wpdb->last_error]);
    }

    return [
        'success' => true,
        'message' => 'Recipe added successfully!',
        'recipe_id' => $wpdb->insert_id,
    ];
}

function get_user_recipes(WP_REST_Request $request) {
    global $wpdb;
	
	$user = wp_get_current_user();
	
	if (!$user) {
        return new WP_Error('unauthorized', 'Invalid or expired token', ['status' => 401]);
    }
	
    $table_name = $wpdb->prefix . 'recipes';
    $results = $wpdb->get_results($wpdb->prepare(
        "SELECT * FROM $table_name WHERE user_id = %d ORDER BY created_at DESC",
        $user->ID
    ));

    return ['success' => true, 'data' => $results];
}

function get_recipe(WP_REST_Request $request) {
	global $wpdb;
	
	$user = wp_get_current_user();
	
	if (!$user) {
        return new WP_Error('unauthorized', 'Invalid or expired token', ['status' => 401]);
    }
	
	$recipe_id = intval($request->get_param('id'));
	
	if (!$recipe_id) {
        return new WP_Error('missing_id', 'Recipe ID is required', ['status' => 400]);
    }
	
	$recipes_table = $wpdb->prefix . 'recipes';
	$recipe = $wpdb->get_row($wpdb->prepare("SELECT * FROM $recipes_table WHERE id = %d", $recipe_id));
	
	if (!$recipe) {
        return new WP_Error('not_found', 'Recipe not found', ['status' => 404]);
    }
	
	$favorites_table = $wpdb->prefix . 'recipe_favorites';
	$favorite_exists = $wpdb->get_var($wpdb->prepare("SELECT COUNT(*) FROM $favorites_table WHERE recipe_id = %d AND user_id = %d", $recipe_id, $user->ID));
	$is_favorite = ($favorite_exists > 0);
	
	return [
        'success' => true,
        'data' => [
            'title' => $recipe->title,
            'description' => $recipe->description,
            'image_url' => $recipe->image_url,
			'user_id' => (int)($recipe->user_id),
			'favorite' => $is_favorite
        ]
    ];
}

function toggle_favorite(WP_REST_Request $request) {
    global $wpdb;
	
	$user = wp_get_current_user();
	
	if (!$user) {
        return new WP_Error('unauthorized', 'Invalid or expired token', ['status' => 401]);
    }
	
	$params = $request->get_json_params();
	if (empty($params)) {
        $params = $request->get_params();
    }
	
	if (!isset($params['recipe_id']))
	{
		return new WP_Error('missing_params', 'recipe_id or user_id missing', ['status' => 400]);
	}
	
    $recipe_id = intval($params['recipe_id']);
    $user_id   = $user->ID;
	
	$recipes_table = $wpdb->prefix . 'recipes';
	$favorites_table = $wpdb->prefix . 'recipe_favorites';
	$favorite_exists = $wpdb->get_var($wpdb->prepare("SELECT COUNT(*) FROM $favorites_table WHERE recipe_id = %d AND user_id = %d", $recipe_id, $user_id));
	$is_favorite = ($favorite_exists > 0);

    if ($request->get_method() === 'POST') {
		if (!$is_favorite)
		{
			$wpdb->replace($favorites_table, [
				'recipe_id' => $recipe_id,
				'user_id'   => $user_id,
				'created_at' => current_time('mysql'),
			]);
			
			$wpdb->query(
				$wpdb->prepare(
					"UPDATE $recipes_table
					SET favorites_count = favorites_count + 1
					WHERE id = %d",
					$recipe_id
				)
			);
		}
        
		return [
			'success' => true,
			'data' => ['favorite_status' => true]
		];
    } else if ($request->get_method() === 'DELETE') {
		if ($is_favorite)
		{
			$wpdb->delete($favorites_table, [
				'recipe_id' => $recipe_id,
				'user_id'   => $user_id,
			]);
			
			$wpdb->query(
				$wpdb->prepare(
					"UPDATE $recipes_table
					 SET favorites_count = favorites_count - 1
					 WHERE id = %d AND favorites_count > 0",
					$recipe_id
				)
			);
		}
       
		return [
			'success' => true,
			'data' => ['favorite_status' => false]
		];
    }
}

function search_recipes(WP_REST_Request $request) {
    global $wpdb;
	
	$user = wp_get_current_user();
	
	if (!$user) {
        return new WP_Error('unauthorized', 'Invalid or expired token', ['status' => 401]);
    }
	
	$query = sanitize_text_field($request->get_param('query'));
	
    $table_name = $wpdb->prefix . 'recipes';
    $results = $wpdb->get_results($wpdb->prepare(
		"SELECT * FROM $table_name
		WHERE title LIKE %s OR description LIKE %s
		LIMIT 20",
		'%' . $wpdb->esc_like($query) . '%',
		'%' . $wpdb->esc_like($query) . '%'
    ));

    return ['success' => true, 'data' => $results];
}