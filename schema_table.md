# Table Schema

## Team
- **TeamId**
- teamName
- teamOwner
- teamPhoto
- membership = list membership 
- sections = list section

### Membership (basic info of user)
- MemberId = UserId
- username
- email
- userPhoto

## User
- UserId
- username
- email
- userPhoto


### Section
- **SectionId**
- sectionName
- tasks_infos = list of TaskInfos

#### TaskInfos
- TaskInfosId = TaskId
- taskName
- deadline
- status