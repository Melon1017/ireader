@Override
	public ResultVO grantPrivilege(JSONObject privilege) throws Exception {
		try {
			String roleId = privilege.getString("roleid");
			// 查出所有权限项
			JSONArray sysFunctionItems = sysFuntionItemDAO.listAllFuntionItem();
			Map<String, Long> sysFunctionMap = new HashMap<>();
			Map<String, Long> billcurrentFunctionMap = new HashMap<>();
			// 遍历改成以funcid,valulegroup 为key,vaueFunction 为value的Map结构更方便处理
			for (Object sysFunctionObj : sysFunctionItems) {
				JSONObject fuctionJSON = (JSONObject) sysFunctionObj;
				sysFunctionMap.put(fuctionJSON.getString("funcid") + "#" + fuctionJSON.getString("valuegroup"),
						fuctionJSON.getLong("valueindex"));
			}
			// 根据角色查询所有菜单所现在拥有的权限
			JSONArray roleBillFuncArray = sysRoleBillDAO.findByBillCodeAndRoleId(roleId);
			// 遍历改成以funcid,valulegroup 为key,vaueFunction 为value的Map结构更方便处理
			for (Object roleBillObj : roleBillFuncArray) {
				JSONObject roleBillObjJSON = (JSONObject) roleBillObj;
				String privileges=roleBillObjJSON.getString("privileges");
				String[] groupedPrivilegesArray=privileges.split(",");
				for(int i=0;i<groupedPrivilegesArray.length;i++) {
					billcurrentFunctionMap.put(
							roleBillObjJSON.getString("billcode") + "#" + i,
							Long.parseLong(groupedPrivilegesArray[i]));
				}
				
			}
			JSONArray privileges = privilege.getJSONArray("privileges");
			List<Object[]> updateParamsList = new ArrayList<>();
			List<Object[]> addParamsList = new ArrayList<>();
			for (Object privilegeObject : privileges) {
				Map<String, Object> privilegeBillMap = (Map<String, Object>) privilegeObject;
				Object[] rowPrama = new Object[5];
				String billCode = privilegeBillMap.get("billcode").toString();

				Map<String, Object> privilegesMap = (Map<String, Object>) privilegeBillMap.get("privilegeitems");
				boolean newgrant = false;
				Long oldPrivilege=0L;
				Long newPrivilege=0L;
				Map<String, Long> billFunctionMap = new HashMap<>();
				for (String key : privilegesMap.keySet()) {
					Map<String,Object>  privilegeItem=(Map<String, Object>) privilegesMap.get(key);
					String valueGroupfromKey =privilegeItem.get("valuegroup").toString();// valuegroup和权限id通过#连接在一起组成key。这一行只取key的valuegroup部分
					if(billcurrentFunctionMap.get(billCode + "#" + valueGroupfromKey)==null) {
						newgrant=true;
					}
				    oldPrivilege = billcurrentFunctionMap.get(billCode + "#" + valueGroupfromKey) == null ? 0L
							: billcurrentFunctionMap.get(billCode + "#" + valueGroupfromKey);
					newPrivilege = Boolean.parseBoolean(privilegeItem.get("available").toString())
							? PrivilegeUtil.addPrivilege(newPrivilege, sysFunctionMap.get(key+"#"+valueGroupfromKey))
							: PrivilegeUtil.removePrivilege(newPrivilege, sysFunctionMap.get(key+"#"+valueGroupfromKey));
					billFunctionMap.put(billCode + "#" + valueGroupfromKey, newPrivilege);
					rowPrama[0] = newPrivilege;
					rowPrama[3] = valueGroupfromKey;
					rowPrama[4] = new Date();

				}
				rowPrama[1] = roleId;
				rowPrama[2] = billCode;
				if(!newgrant&&oldPrivilege.longValue()==newPrivilege.longValue()) {
					continue;
				}
				if (newgrant) {
					addParamsList.add(rowPrama);
				} else {
					updateParamsList.add(rowPrama);
				}
			}
			if(updateParamsList!=null&&!updateParamsList.isEmpty()) {
				sysRoleBillDAO.batchUpdateRoleBill(updateParamsList);
			}
			if(addParamsList!=null&&!addParamsList.isEmpty()) {
				sysRoleBillDAO.insertRoleBill(addParamsList);
			}
			return ResultVO.successResult(true);
		} catch (Exception e) {
			log.error("授权出现异常", e);
			throw new Exception(e);
		}
	}
