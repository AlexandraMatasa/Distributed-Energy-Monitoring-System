function performRequest(request, callback) {
    fetch(request)
        .then(async (response) => {
            let data = {};
            let text = "";

            try {
                text = await response.text();

                if (text && (text.trim().startsWith("{") || text.trim().startsWith("["))) {
                    data = JSON.parse(text);
                } else if (text) {
                    data = { message: text.trim() };
                }
            } catch (e) {
                console.error("Error reading response:", e);
                data = { message: text?.trim() || "Unknown error" };
            }

            if (response.ok) {
                callback(data, response.status, null);
            } else {
                let errorMsg = `Error ${response.status}`;
                if (data?.message) {
                    errorMsg = data.message;
                } else if (data?.error) {
                    errorMsg = data.error;
                }

                if (data?.validationErrors && data.validationErrors.password) {
                    errorMsg = data.validationErrors.password;
                }

                callback(null, response.status, { message: errorMsg });
            }
        })
        .catch((err) => {
            console.error("Network error:", err);
            callback(null, 0, { message: err.message || "Network error" });
        });
}

export default {
    performRequest
};
