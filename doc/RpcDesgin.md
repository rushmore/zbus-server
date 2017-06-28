##Rpc Design

	Request {
		+ method: string   <required>
		+ params: object[] <optional>
		+ module: string   <optional>
	}
	
	Response {
		+ result: dynamic
		+ error: error
	}